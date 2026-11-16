package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Speedometer
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.andromeda.device.DeviceSensors
import com.kylecorry.andromeda.device.FlowSatelliteStatus
import com.kylecorry.andromeda.device.SensorAccuracy
import kotlin.time.TimeSource
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.altitude.BarometricAltimeter
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.compass.Compass
import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.compass.FilteredCompass
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.andromeda.sense.location.filters.GPSGaussianAltitudeFilter
import com.kylecorry.andromeda.sense.location.filters.GPSPassThroughAltitudeFilter
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.mock.MockBarometer
import com.kylecorry.andromeda.sense.mock.MockGyroscope
import com.kylecorry.andromeda.sense.orientation.CustomGeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.andromeda.sense.orientation.filter.FilteredOrientationSensor
import com.kylecorry.andromeda.sense.orientation.filter.LowPassOrientationSensorFilter
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.sensors.altimeter.AltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.DigitalElevationModel
import com.kylecorry.trail_sense.shared.sensors.altimeter.GaussianAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.barometer.CalibratedBarometer
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityCompassWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityOrientationWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MockCompass
import com.kylecorry.trail_sense.shared.sensors.gps.CachedLocationGPS
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.shared.sensors.gps.InactiveGPS
import com.kylecorry.trail_sense.shared.sensors.gps.MockedGPS
import com.kylecorry.trail_sense.shared.sensors.hygrometer.MockHygrometer
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.CalibratedThermometerWrapper
import com.kylecorry.trail_sense.shared.sensors.thermometer.HistoricThermometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

@Serializable
internal data class SharedGpsCacheEntry(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Float = 0f,
    val bearing: Float? = null,
    val speed: Float = 0f,
    val updateTimeMillis: Long = 0L,
    val horizontalAccuracy: Float? = null,
    val verticalAccuracy: Float? = null
)

internal object SharedSensorCache {
    const val LAST_GPS_KEY = "last_gps"
    const val LAST_ALTITUDE_KEY = "last_altitude_2"
    const val LAST_ALTITUDE_UPDATE_KEY = "last_altitude_update_2"
    private val json = Json { ignoreUnknownKeys = true }

    fun readGps(preferences: Preferences): SharedGpsCacheEntry? {
        val raw = preferences.getString(LAST_GPS_KEY) ?: return null
        return runCatching { json.decodeFromString<SharedGpsCacheEntry>(raw) }.getOrNull()
    }

    fun writeGps(preferences: Preferences, gps: GpsSensor) {
        if (!gps.hasValidReading || gps.location == Coordinate.zero) return
        val entry = SharedGpsCacheEntry(
            latitude = gps.location.latitude,
            longitude = gps.location.longitude,
            altitude = gps.altitude,
            bearing = (gps.rawBearing ?: gps.bearing?.value)?.takeIf { it.isFinite() },
            speed = gps.speed.value,
            updateTimeMillis = Clock.System.now().toEpochMilliseconds(),
            horizontalAccuracy = gps.horizontalAccuracy,
            verticalAccuracy = gps.verticalAccuracy
        )
        preferences.putString(LAST_GPS_KEY, json.encodeToString(entry))
    }
}

internal enum class SharedGpsSource {
    Override,
    Timezone,
    Device,
    Cache
}

internal fun selectGpsSource(
    userPrefs: UserPreferences,
    hasPermission: Boolean,
    locationAvailable: Boolean,
    useCache: Boolean
): SharedGpsSource {
    if (userPrefs.gps.locationSource == GPSLocationSource.Manual || (!hasPermission && userPrefs.gps.hasLocationOverride)) {
        return SharedGpsSource.Override
    }
    if (!hasPermission) {
        return SharedGpsSource.Timezone
    }
    if (!useCache && locationAvailable) {
        return SharedGpsSource.Device
    }
    return SharedGpsSource.Cache
}

/**
 * Picks cross-platform Andromeda DeviceSensors (or preferences-based fallbacks) from shared settings.
 */
class SharedSensorService(
    private val preferences: Preferences,
    private val userPrefs: UserPreferences,
    private val device: DeviceProfile,
    private val locationPermissionCheck: (background: Boolean) -> Boolean = { true },
    private val locationAvailableCheck: () -> Boolean = { true },
    private val activityRecognitionCheck: () -> Boolean = { true },
    private val sensors: DeviceSensors = DeviceSensors()
) : SensorService {

    private val sensorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun getGPS(
        frequency: Duration,
        useCache: Boolean,
        tag: String?
    ): ISatelliteGPS {
        return when (selectGpsSource(userPrefs, hasLocationPermission(), locationAvailableCheck(), useCache)) {
            SharedGpsSource.Override -> SharedOverrideGPS(userPrefs, frequency)
            SharedGpsSource.Timezone -> SharedTimezoneGPS(frequency)
            SharedGpsSource.Device -> SharedSatelliteGPS(
                gps = sensors.gps(sensorScope, frequency, mapGpsAccuracy()),
                satellitesSensor = sensors.satelliteStatusSensor(sensorScope),
                preferences = preferences
            )
            SharedGpsSource.Cache -> SharedCachedGPS(preferences, userPrefs, frequency)
        }
    }

    override fun hasLocationPermission(background: Boolean): Boolean {
        return locationPermissionCheck(background)
    }

    override fun getPedometer(): Pedometer {
        return if (activityRecognitionCheck()) {
            sensors.pedometer(sensorScope, ENVIRONMENT_SENSOR_INTERVAL)
        } else {
            MockPedometer()
        }
    }

    override fun getSpeedometer(gps: GpsSensor?, tag: String?): Speedometer {
        return when (userPrefs.navigation.speedometerMode) {
            NavigationPreferences.SpeedometerMode.Backtrack -> BacktrackSpeedometer()
            NavigationPreferences.SpeedometerMode.GPS -> gps ?: getGPS(tag = tag)
            NavigationPreferences.SpeedometerMode.CurrentPace -> CurrentPaceSpeedometer(
                getPedometer(),
                StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )
            NavigationPreferences.SpeedometerMode.AveragePace -> AveragePaceSpeedometer(
                getAppService<IStepTrackerService>(),
                StrideLengthPaceCalculator(userPrefs.pedometer.strideLength),
                userPrefs.pedometer
            )
        }
    }

    private fun getGPSAltimeter(gps: GpsSensor? = null, frequency: Duration, tag: String?): Altimeter {
        val mode = userPrefs.altimeterMode
        return when {
            mode == UserPreferences.AltimeterMode.Override -> SharedOverrideAltimeter(userPrefs)
            mode.usesDem -> DigitalElevationModel(gps ?: getGPS(frequency, tag = tag))
            !locationAvailableCheck() || !hasLocationPermission() -> SharedCachedAltimeter(preferences, userPrefs)
            else -> gps ?: getGPS(frequency, tag = tag)
        }
    }

    override fun getAltimeter(
        preferGPS: Boolean,
        gps: GpsSensor?,
        frequency: Duration,
        tag: String?
    ): Altimeter {
        if (preferGPS) {
            return SharedCachingAltimeterWrapper(
                preferences,
                GaussianAltimeterWrapper(getGPSAltimeter(gps, frequency, tag), userPrefs.altimeterSamples)
            )
        }

        val hasBarometer = device.hasBarometer && userPrefs.weather.hasBarometer
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return SharedOverrideAltimeter(userPrefs)
        }
        if (mode == UserPreferences.AltimeterMode.Barometer && hasBarometer) {
            return SharedCachingAltimeterWrapper(
                preferences,
                BarometricAltimeter(
                    getBarometer(),
                    seaLevelPressure = Pressure.hpa(userPrefs.seaLevelPressureOverride)
                )
            )
        }
        if (mode == UserPreferences.AltimeterMode.DigitalElevationModel) {
            return SharedCachingAltimeterWrapper(
                preferences,
                DigitalElevationModel(gps ?: getGPS(frequency, tag = tag))
            )
        }

        if (!locationAvailableCheck() || !hasLocationPermission()) {
            return if (mode == UserPreferences.AltimeterMode.GPSBarometer && hasBarometer) {
                SharedCachingAltimeterWrapper(
                    preferences,
                    SharedAutoInitializeBarometricAltimeter(
                        getBarometer(),
                        SharedCachedAltimeter(preferences, userPrefs)
                    )
                )
            } else {
                SharedCachedAltimeter(preferences, userPrefs)
            }
        }

        var resolvedGps = gps ?: getGPS(frequency, tag = tag)
        if (mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) {
            resolvedGps = DigitalElevationModel(resolvedGps)
        }

        return if ((mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) && hasBarometer) {
            SharedCachingAltimeterWrapper(
                preferences,
                FusedAltimeter(
                    resolvedGps,
                    getBarometer(),
                    preferences,
                    gpsFilter = if (mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) {
                        GPSPassThroughAltitudeFilter()
                    } else {
                        GPSGaussianAltitudeFilter(userPrefs.altimeterSamples)
                    },
                    useContinuousCalibration = userPrefs.altimeter.useFusedAltimeterContinuousCalibration,
                    recalibrationInterval = userPrefs.altimeter.fusedAltimeterForcedRecalibrationInterval,
                    useMSLAltitude = false
                )
            )
        } else {
            SharedCachingAltimeterWrapper(
                preferences,
                GaussianAltimeterWrapper(resolvedGps, userPrefs.altimeterSamples)
            )
        }
    }

    override fun hasCompass(): Boolean {
        return !userPrefs.debug.compassUnavailable && device.hasCompass
    }

    override fun hasGyroscope(): Boolean {
        return device.hasGyroscope
    }

    override fun getCompass(
        orientationSensor: OrientationSensor?,
        delay: Int
    ): CompassSensor {
        if (!hasCompass()) {
            return MockCompass()
        }
        val useTrueNorth = userPrefs.compass.useTrueNorth
        val compass = if (orientationSensor == null && userPrefs.compass.source == CompassSource.Orientation) {
            val rawCompass = sensors.compass(sensorScope, motionInterval(delay), SensorAccuracy.HighPrecision)
            rawCompass.useTrueNorth = useTrueNorth
            FilteredCompass(
                rawCompass,
                MovingAverageFilter((userPrefs.compass.compassSmoothing * 4).coerceAtLeast(1))
            )
        } else {
            Compass(
                orientationSensor ?: getOrientation(delay),
                useTrueNorth = useTrueNorth,
                surfaceRotation = 0
            )
        }
        return MagQualityCompassWrapper(
            compass,
            getMagnetometer(filtered = false, delay = delay)
        )
    }

    override fun getOrientation(delay: Int): OrientationSensor {
        val interval = motionInterval(delay)
        val smoothing = userPrefs.compass.compassSmoothing
        val baseSensor: OrientationSensor = if (
            !hasGyroscope() ||
            userPrefs.compass.source == CompassSource.CustomMagnetometer ||
            userPrefs.compass.source == CompassSource.GeomagneticRotationVector
        ) {
            CustomGeomagneticRotationSensor(
                getMagnetometer(filtered = true, delay = delay),
                getGravity(delay),
                onlyUseMagnetometerQuality = true
            )
        } else {
            sensors.orientationSensor(sensorScope, interval, SensorAccuracy.HighPrecision)
        }

        val smoothed = if (smoothing <= 1) {
            baseSensor
        } else {
            val alpha = Interpolation.map((1 - smoothing / 100f).pow(2), 0f, 1f, 0.005f, 1f)
            FilteredOrientationSensor(baseSensor, LowPassOrientationSensorFilter(alpha, true))
        }

        return MagQualityOrientationWrapper(
            smoothed,
            getMagnetometer(filtered = false, delay = delay)
        )
    }

    override fun getDeviceOrientationSensor(): DeviceOrientation {
        return DeviceOrientation(getGravity(SensorService.MOTION_SENSOR_DELAY))
    }

    override fun getBarometer(calibrated: Boolean): Barometer {
        if (!device.hasBarometer || !userPrefs.weather.hasBarometer) {
            return MockBarometer()
        }
        val raw = sensors.barometer(sensorScope, ENVIRONMENT_SENSOR_INTERVAL)
        val calibratedSensor = if (calibrated) {
            CalibratedBarometer(raw, userPrefs.weather.barometerOffset)
        } else {
            raw
        }
        return FilteredBarometer(calibratedSensor, 3) { LowPassFilter(0.1f, it) }
    }

    override fun getThermometer(calibrated: Boolean): Thermometer {
        val thermometer = when (userPrefs.thermometer.source) {
            ThermometerSource.Historic -> HistoricThermometer()
            ThermometerSource.Sensor -> sensors.thermometer(sensorScope, ENVIRONMENT_SENSOR_INTERVAL)
        }
        return if (calibrated) {
            CalibratedThermometerWrapper(thermometer, userPrefs.thermometer.calibrator)
        } else {
            thermometer
        }
    }

    override fun getHygrometer(): Hygrometer {
        return if (device.hasHygrometer) {
            sensors.hygrometer(sensorScope, ENVIRONMENT_SENSOR_INTERVAL)
        } else {
            MockHygrometer()
        }
    }

    override fun getCellSignal(removeUnregisteredSignals: Boolean): CellSignalSensor {
        return MockCellSignalSensor()
    }

    override fun getGravity(delay: Int): Accelerometer {
        return sensors.gravitySensor(sensorScope, motionInterval(delay), SensorAccuracy.HighPrecision)
    }

    override fun getMagnetometer(filtered: Boolean, delay: Int): Magnetometer {
        val raw = sensors.magnetometer(sensorScope, motionInterval(delay), SensorAccuracy.HighPrecision)
        return if (filtered) {
            LowPassMagnetometer(raw, MAGNETOMETER_LOW_PASS)
        } else {
            raw
        }
    }

    override fun getGyroscope(delay: Int): OrientationSensor {
        if (!hasGyroscope()) {
            return MockGyroscope()
        }
        return sensors.gyroscope(sensorScope, motionInterval(delay), SensorAccuracy.HighPrecision)
    }

    override fun getAccelerometer(delay: Int): Accelerometer {
        val raw = sensors.accelerometer(sensorScope, motionInterval(delay), SensorAccuracy.HighPrecision)
        return LowPassAccelerometer(raw, ACCELEROMETER_LOW_PASS)
    }

    override fun getLightSensor(): LightSensor {
        return sensors.lightSensor(sensorScope, ENVIRONMENT_SENSOR_INTERVAL)
    }

    private fun mapGpsAccuracy(): SensorAccuracy = when (userPrefs.gps.powerMode) {
        GPSPowerMode.Low -> SensorAccuracy.PowerSave
        GPSPowerMode.Balanced -> SensorAccuracy.Balanced
        GPSPowerMode.High, GPSPowerMode.Inherit -> SensorAccuracy.HighPrecision
    }

    private fun motionInterval(delay: Int): Duration = when (delay) {
        0 -> 20.milliseconds
        1 -> 20.milliseconds
        2 -> 60.milliseconds
        else -> ENVIRONMENT_SENSOR_INTERVAL
    }

    private companion object {
        val ENVIRONMENT_SENSOR_INTERVAL = 200.milliseconds
        const val MAGNETOMETER_LOW_PASS = 0.3f
        const val ACCELEROMETER_LOW_PASS = 0.1f
    }
}

private class SharedSatelliteGPS(
    private val gps: GpsSensor,
    private val satellitesSensor: FlowSatelliteStatus,
    private val preferences: Preferences
) : SharedSensor(), ISatelliteGPS {

    override val hasValidReading: Boolean get() = gps.hasValidReading
    override val quality: Quality get() = gps.quality
    override val location: Coordinate get() = gps.location
    override val speed: Speed get() = gps.speed
    override val speedAccuracy: Float? get() = gps.speedAccuracy
    override val horizontalAccuracy: Float? get() = gps.horizontalAccuracy
    override val verticalAccuracy: Float? get() = gps.verticalAccuracy
    override val altitude: Float get() = gps.altitude
    override val mslAltitude: Float? get() = gps.mslAltitude ?: gps.altitude
    override val bearing: Bearing? get() = gps.bearing
    override val rawBearing: Float? get() = gps.rawBearing
    override val bearingAccuracy: Float? get() = gps.bearingAccuracy
    override val satellites: Int? get() = satellitesSensor.satellites
    override val satelliteDetails: List<Satellite>? get() = satellitesSensor.satelliteDetails

    override fun startImpl() {
        gps.start(this::onGpsReading)
        satellitesSensor.start(this::onSatReading)
    }

    override fun stopImpl() {
        gps.stop(this::onGpsReading)
        satellitesSensor.stop(this::onSatReading)
    }

    private fun onGpsReading(): Boolean {
        SharedSensorCache.writeGps(preferences, gps)
        setEventTimeFrom(gps)
        notifyListeners()
        return true
    }

    private fun onSatReading(): Boolean {
        notifyListeners()
        return true
    }
}

internal class SharedOverrideGPS(
    private val userPrefs: UserPreferences,
    updateFrequency: Duration = 1000.milliseconds
) : IntervalSensor(updateFrequency), ISatelliteGPS, InactiveGPS {
    private val uptimeStart = TimeSource.Monotonic.markNow()
    override val location: Coordinate get() = userPrefs.gps.locationOverride
    override val speed: Speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    override val speedAccuracy: Float? = null
    override val verticalAccuracy: Float? = null
    override val horizontalAccuracy: Float? = null
    override val satellites: Int = 0
    override val hasValidReading: Boolean = true
    override val altitude: Float get() = userPrefs.altitudeOverride
    override val bearing: Bearing? = null
    override val bearingAccuracy: Float? = null
    override val mslAltitude: Float get() = altitude
    override val rawBearing: Float? = null
    override val satelliteDetails: List<Satellite>? = null
    override var timestamp: Instant
        get() = Clock.System.now()
        set(_) {}
    override var eventTimeElapsedNanos: Long
        get() = uptimeStart.elapsedNow().inWholeNanoseconds
        set(_) {}
}

internal class SharedTimezoneGPS(
    updateFrequency: Duration = 1000.milliseconds
) : IntervalSensor(updateFrequency), ISatelliteGPS, InactiveGPS, MockedGPS {
    private val hooks = Hooks()
    private val uptimeStart = TimeSource.Monotonic.markNow()

    override val location: Coordinate
        get() {
            val zone = TimeZone.currentSystemDefault()
            return hooks.memo("location", zone) {
                Time.getLocationFromTimeZone(zone)
            }
        }
    override val speed: Speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    override val speedAccuracy: Float? = null
    override val verticalAccuracy: Float? = null
    override val horizontalAccuracy: Float? = null
    override val satellites: Int = 0
    override val hasValidReading: Boolean = true
    override val altitude: Float = 0f
    override val bearing: Bearing? = null
    override val bearingAccuracy: Float? = null
    override val mslAltitude: Float = 0f
    override val rawBearing: Float? = null
    override val satelliteDetails: List<Satellite>? = null
    override var timestamp: Instant
        get() = Clock.System.now()
        set(_) {}
    override var eventTimeElapsedNanos: Long
        get() = uptimeStart.elapsedNow().inWholeNanoseconds
        set(_) {}
}

internal class SharedCachedGPS(
    private val preferences: Preferences,
    private val userPrefs: UserPreferences,
    private val updateFrequency: Duration = 1000.milliseconds
) : SharedSensor(), ISatelliteGPS, InactiveGPS, MockedGPS, CachedLocationGPS {

    private val cached: SharedGpsCacheEntry?
        get() = SharedSensorCache.readGps(preferences)

    private val timer = CoroutineTimer {
        setEventTimeToNow()
        notifyListeners()
    }

    override val location: Coordinate
        get() {
            val c = cached
            val override = userPrefs.gps.locationOverride
            return Coordinate(c?.latitude ?: override.latitude, c?.longitude ?: override.longitude)
        }
    override val speed: Speed
        get() = Speed.from(cached?.speed ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
    override val speedAccuracy: Float? = null
    override val verticalAccuracy: Float? get() = cached?.verticalAccuracy
    override val horizontalAccuracy: Float? get() = cached?.horizontalAccuracy
    override val satellites: Int = 0
    override val hasValidReading: Boolean = true
    override val altitude: Float get() = cached?.altitude ?: userPrefs.altitudeOverride
    override val bearing: Bearing? = null
    override val bearingAccuracy: Float? = null
    override val mslAltitude: Float get() = altitude
    override val rawBearing: Float? = null
    override val satelliteDetails: List<Satellite>? = null

    override fun startImpl() {
        timer.interval(updateFrequency)
    }

    override fun stopImpl() {
        timer.stop()
    }
}

internal class SharedCachingAltimeterWrapper(
    private val preferences: Preferences,
    override val altimeter: Altimeter
) : SharedSensor(), AltimeterWrapper {

    override val altitudeAccuracy: Float?
        get() = (altimeter as? AltimeterWrapper)?.altitudeAccuracy

    override val altitude: Float
        get() = if (altimeter.hasValidReading || altimeter.altitude != 0f) {
            altimeter.altitude
        } else {
            preferences.getFloat(SharedSensorCache.LAST_ALTITUDE_KEY) ?: 0f
        }

    override val hasValidReading: Boolean get() = altimeter.hasValidReading
    override val quality: Quality get() = altimeter.quality

    override fun startImpl() {
        altimeter.start(this::onReading)
    }

    override fun stopImpl() {
        altimeter.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        preferences.putFloat(SharedSensorCache.LAST_ALTITUDE_KEY, altitude)
        preferences.putInstant(SharedSensorCache.LAST_ALTITUDE_UPDATE_KEY, Clock.System.now())
        setEventTimeFrom(altimeter)
        notifyListeners()
        return true
    }
}

internal class SharedCachedAltimeter(
    private val preferences: Preferences,
    private val userPrefs: UserPreferences,
    private val updateFrequency: Duration = 20.milliseconds
) : SharedSensor(), Altimeter {
    override val hasValidReading: Boolean = true
    override val altitude: Float
        get() = preferences.getFloat(SharedSensorCache.LAST_ALTITUDE_KEY) ?: userPrefs.altitudeOverride

    private val timer = CoroutineTimer {
        setEventTimeToNow()
        notifyListeners()
    }

    override fun startImpl() {
        timer.interval(updateFrequency)
    }

    override fun stopImpl() {
        timer.stop()
    }
}

internal class SharedOverrideAltimeter(
    private val userPrefs: UserPreferences,
    private val updateFrequency: Duration = 20.milliseconds
) : SharedSensor(), Altimeter {
    override val hasValidReading: Boolean = true
    override val altitude: Float get() = userPrefs.altitudeOverride

    private val timer = CoroutineTimer {
        setEventTimeToNow()
        notifyListeners()
    }

    override fun startImpl() {
        timer.interval(updateFrequency)
    }

    override fun stopImpl() {
        timer.stop()
    }
}

private class SharedAutoInitializeBarometricAltimeter(
    private val barometer: Barometer,
    private val cachedAltimeter: SharedCachedAltimeter
) : SharedSensor(), Altimeter {
    private var isRunning = false
    private var altimeter: Altimeter? = null

    override val hasValidReading: Boolean get() = altimeter?.hasValidReading == true
    override val altitude: Float get() = altimeter?.altitude ?: cachedAltimeter.altitude

    override fun startImpl() {
        isRunning = true
        barometer.start(this::onBarometerUpdate)
    }

    override fun stopImpl() {
        isRunning = false
        barometer.stop(this::onBarometerUpdate)
        altimeter?.stop(this::onAltimeterUpdate)
        altimeter = null
    }

    private fun onBarometerUpdate(): Boolean {
        val pressure = barometer.pressure
        if (pressure == 0f) return true
        val lastAltitude = cachedAltimeter.altitude
        val seaLevelPressure = if (lastAltitude != 0f) {
            Meteorology.getSeaLevelPressure(Pressure.hpa(pressure), Distance.meters(lastAltitude))
        } else {
            Pressure.hpa(1013.25f)
        }
        if (isRunning) {
            altimeter?.stop(this::onAltimeterUpdate)
            altimeter = BarometricAltimeter(barometer, seaLevelPressure)
            altimeter?.start(this::onAltimeterUpdate)
        }
        return false
    }

    private fun onAltimeterUpdate(): Boolean {
        altimeter?.let { setEventTimeFrom(it) }
        notifyListeners()
        return true
    }
}
