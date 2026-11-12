package com.kylecorry.trail_sense.shared.sensors




import com.kylecorry.andromeda.sense.accelerometer.AndroidAccelerometer
import com.kylecorry.andromeda.sense.barometer.AndroidBarometer
import com.kylecorry.andromeda.sense.magnetometer.AndroidMagnetometer
import com.kylecorry.andromeda.sense.orientation.AndroidGyroscope
import com.kylecorry.andromeda.sense.light.AndroidLightSensor
import com.kylecorry.andromeda.sense.pedometer.AndroidPedometer
import com.kylecorry.andromeda.sense.hygrometer.AndroidHygrometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.kylecorry.andromeda.battery.AndroidBattery
import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.sensors.Speedometer
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.andromeda.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.altitude.BarometricAltimeter
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.barometer.Barometer

import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer
import com.kylecorry.andromeda.sense.light.LightSensor
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.andromeda.sense.location.filters.GPSGaussianAltitudeFilter
import com.kylecorry.andromeda.sense.location.filters.GPSPassThroughAltitudeFilter

import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.mock.MockBarometer
import com.kylecorry.andromeda.sense.mock.MockGyroscope
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.andromeda.sense.orientation.GameRotationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationSensor

import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.andromeda.sense.temperature.AmbientThermometer
import com.kylecorry.andromeda.sense.temperature.LegacyThermometer
import com.kylecorry.andromeda.signal.CellSignalOptions
import com.kylecorry.andromeda.signal.CellSignalSensor
import com.kylecorry.sol.math.filters.LowPassFilter
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.shared.sensors.gps.ISatelliteGPS
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.compass.MockCompass
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.altimeter.AutoInitializeBarometricAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachedAltimeter
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachingAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.DigitalElevationModel
import com.kylecorry.trail_sense.shared.sensors.altimeter.GaussianAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.altimeter.OverrideAltimeter
import com.kylecorry.trail_sense.shared.sensors.barometer.CalibratedBarometer
import com.kylecorry.trail_sense.shared.sensors.gps.GPSSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSSourceSelector
import com.kylecorry.trail_sense.shared.sensors.gps.TimezoneGPS
import com.kylecorry.trail_sense.shared.sensors.hygrometer.MockHygrometer
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider
import com.kylecorry.trail_sense.shared.sensors.speedometer.BacktrackSpeedometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.CalibratedThermometerWrapper
import com.kylecorry.trail_sense.shared.sensors.thermometer.HistoricThermometer
import com.kylecorry.trail_sense.shared.sensors.thermometer.ThermometerSource
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.AveragePaceSpeedometer
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.CurrentPaceSpeedometer
import kotlin.time.Duration

// Maybe use the concept of a use case
// Ex. SensorPurpose.Background, SensorPurpose.Calibration, SensorPurpose.Diagnostics
// Using those, it can adjust settings to be more appropriate for the use case

/**
 * Picks Android sensors, or stand-ins when the device or the user's settings rule one out.
 */
class AndroidSensorService(ctx: Context) : SensorService {

    private var context = ctx.applicationContext
    private val userPrefs by lazy { UserPreferences(context) }
    private val gpsSourceSelector by lazy { GPSSourceSelector(context) }

    override fun getGPS(
        frequency: Duration,
        useCache: Boolean,
        tag: String?
    ): ISatelliteGPS {
        return when (gpsSourceSelector.getSource(useCache)) {
            GPSSource.Override -> OverrideGPS(context, frequency.inWholeMilliseconds)
            GPSSource.Timezone -> TimezoneGPS(frequency.inWholeMilliseconds)
            GPSSource.Device -> CustomGPS(context, frequency, tag)
            GPSSource.Cache -> CachedGPS(context, frequency.inWholeMilliseconds)
        }
    }


    override fun hasLocationPermission(background: Boolean): Boolean {
        return if (background) {
            Permissions.isBackgroundLocationEnabled(context)
        } else {
            Permissions.canGetFineLocation(context)
        }
    }

    override fun getPedometer(): Pedometer {
        return if (Permissions.canRecognizeActivity(context)) {
            AndroidPedometer(context, ENVIRONMENT_SENSOR_DELAY)
        } else {
            MockPedometer()
        }
    }

    override fun getSpeedometer(gps: GpsSensor?, tag: String?): Speedometer {
        return when (userPrefs.navigation.speedometerMode) {
            NavigationPreferences.SpeedometerMode.Backtrack -> BacktrackSpeedometer()
            NavigationPreferences.SpeedometerMode.GPS -> gps ?: getGPS(tag = tag)
            NavigationPreferences.SpeedometerMode.CurrentPace -> CurrentPaceSpeedometer(
                getPedometer(), StrideLengthPaceCalculator(userPrefs.pedometer.strideLength)
            )

            NavigationPreferences.SpeedometerMode.AveragePace -> AveragePaceSpeedometer(
                getAppService<StepTrackerService>(),
                StrideLengthPaceCalculator(userPrefs.pedometer.strideLength),
                userPrefs.pedometer
            )
        }
    }

    private fun getGPSAltimeter(gps: GpsSensor? = null, frequency: Duration, tag: String?): Altimeter {
        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else {
            if (mode.usesDem) {
                return getDigitalElevationModel(gps, frequency, tag)
            }

            if (!GPS.isAvailable(context)) {
                return CachedAltimeter(context)
            }

            return gps ?: getGPS(frequency, tag = tag)
        }
    }

    private fun getDigitalElevationModel(gps: GpsSensor? = null, frequency: Duration, tag: String?): GpsSensor {
        return DigitalElevationModel(
            gps ?: getGPS(frequency, tag = tag)
        )
    }

    override fun getAltimeter(
        preferGPS: Boolean,
        gps: GpsSensor?,
        frequency: Duration,
        tag: String?
    ): Altimeter {
        if (preferGPS) {
            return CachingAltimeterWrapper(
                context, GaussianAltimeterWrapper(
                    getGPSAltimeter(gps, frequency, tag), userPrefs.altimeterSamples
                )
            )
        }

        val hasBarometer = Sensors.hasBarometer(context)

        val mode = userPrefs.altimeterMode

        if (mode == UserPreferences.AltimeterMode.Override) {
            return OverrideAltimeter(context)
        } else if (mode == UserPreferences.AltimeterMode.Barometer && hasBarometer) {
            return CachingAltimeterWrapper(
                context, BarometricAltimeter(
                    getBarometer(),
                    seaLevelPressure = Pressure.hpa(userPrefs.seaLevelPressureOverride)
                )
            )
        } else if (mode == UserPreferences.AltimeterMode.DigitalElevationModel) {
            return CachingAltimeterWrapper(context, getDigitalElevationModel(gps, frequency, tag))
        } else {
            if (!GPS.isAvailable(context)) {
                if (mode == UserPreferences.AltimeterMode.GPSBarometer && hasBarometer) {
                    return CachingAltimeterWrapper(
                        context, AutoInitializeBarometricAltimeter(
                            getBarometer(),
                            CachedAltimeter(context)
                        )
                    )
                }

                return CachedAltimeter(context)
            }

            var gps = gps ?: getGPS(frequency, tag = tag)
            if (mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) {
                gps = getDigitalElevationModel(gps, frequency, tag)
            }

            return if ((mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) && hasBarometer) {
                CachingAltimeterWrapper(
                    context, FusedAltimeter(
                        gps,
                        getBarometer(),
                        PreferencesSubsystem.getInstance(context).preferences,
                        gpsFilter = if (mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer) GPSPassThroughAltitudeFilter() else GPSGaussianAltitudeFilter(
                            userPrefs.altimeterSamples
                        ),
                        useContinuousCalibration = userPrefs.altimeter.useFusedAltimeterContinuousCalibration,
                        recalibrationInterval = userPrefs.altimeter.fusedAltimeterForcedRecalibrationInterval,
                        useMSLAltitude = false

                    )
                )
            } else {
                CachingAltimeterWrapper(
                    context, GaussianAltimeterWrapper(gps, userPrefs.altimeterSamples)
                )
            }
        }
    }

    override fun hasCompass(): Boolean {
        return !isCompassForcedUnavailable() && Sensors.hasCompass(context)
    }

    private fun isCompassForcedUnavailable(): Boolean {
        return userPrefs.debug.compassUnavailable
    }

    override fun hasGyroscope(): Boolean {
        return Sensors.hasGyroscope(context)
    }

    // TODO: Expose a way to see that a disturbance is present
    override fun getCompass(
        orientationSensor: OrientationSensor?,
        delay: Int
    ): CompassSensor {
        if (isCompassForcedUnavailable()) {
            return MockCompass()
        }
        return CompassProvider(context, userPrefs.compass).get(
            delay,
            orientationSensor
        )
    }

    override fun getOrientation(delay: Int): OrientationSensor {
        return CompassProvider(context, userPrefs.compass).getOrientationSensor(delay)
    }

    override fun getDeviceOrientationSensor(): DeviceOrientation {
        // While not technically an environment sensor, it doesn't need to update often - and can match their rate
        return DeviceOrientation(getGravity(ENVIRONMENT_SENSOR_DELAY))
    }

    override fun getBarometer(calibrated: Boolean): Barometer {
        if (!userPrefs.weather.hasBarometer) {
            return MockBarometer()
        }

        val rawBarometer = AndroidBarometer(context, ENVIRONMENT_SENSOR_DELAY)

        val barometer = if (calibrated) CalibratedBarometer(
            rawBarometer,
            userPrefs.weather.barometerOffset
        ) else rawBarometer

        return FilteredBarometer(barometer, 3) {
            LowPassFilter(0.1f, it)
        }
    }

    override fun getThermometer(calibrated: Boolean): Thermometer {
        val thermometer = when (userPrefs.thermometer.source) {
            ThermometerSource.Historic -> HistoricThermometer()
            ThermometerSource.Sensor -> getThermometerSensor()
        }
        return if (calibrated) {
            CalibratedThermometerWrapper(
                thermometer, userPrefs.thermometer.calibrator
            )
        } else {
            thermometer
        }
    }

    @Suppress("DEPRECATION")
    private fun getThermometerSensor(): Thermometer {
        if (Sensors.hasSensor(context, Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            return AmbientThermometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        if (Sensors.hasSensor(context, Sensor.TYPE_TEMPERATURE)) {
            return LegacyThermometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        return AndroidBattery(context)
    }

    override fun getHygrometer(): Hygrometer {
        if (Sensors.hasHygrometer(context)) {
            return AndroidHygrometer(context, ENVIRONMENT_SENSOR_DELAY)
        }

        return MockHygrometer()
    }

    override fun getCellSignal(removeUnregisteredSignals: Boolean): CellSignalSensor {
        if (!hasLocationPermission()) {
            return MockCellSignalSensor()
        }
        return CellSignalFlowSensor(
            CellSignalOptions(
                updateCellCache = userPrefs.cellSignal.populateCache,
                includeUnregistered = !removeUnregisteredSignals,
                pathLossFactor = 0.1f
            )
        )
    }

    override fun getGravity(delay: Int): Accelerometer {
        return if (Sensors.hasSensor(context, Sensor.TYPE_GRAVITY)) {
            GravitySensor(context, delay)
        } else {
            LowPassAccelerometer(AndroidAccelerometer(context, delay))
        }
    }

    override fun getMagnetometer(
        filtered: Boolean,
        delay: Int
    ): Magnetometer {
        return if (filtered) {
            LowPassMagnetometer(AndroidMagnetometer(context, delay))
        } else {
            AndroidMagnetometer(context, delay)
        }
    }

    override fun getGyroscope(delay: Int): OrientationSensor {
        if (!Sensors.hasGyroscope(context)) {
            return MockGyroscope()
        }
        if (Sensors.hasSensor(context, Sensor.TYPE_GAME_ROTATION_VECTOR)) {
            return GameRotationSensor(context, delay)
        }
        return AndroidGyroscope(context, delay)
    }

    override fun getAccelerometer(delay: Int): Accelerometer {
        return AndroidAccelerometer(context, delay)
    }

    override fun getLightSensor(): LightSensor {
        return AndroidLightSensor(context, ENVIRONMENT_SENSOR_DELAY)
    }

    private companion object {
        const val ENVIRONMENT_SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL
    }

}
