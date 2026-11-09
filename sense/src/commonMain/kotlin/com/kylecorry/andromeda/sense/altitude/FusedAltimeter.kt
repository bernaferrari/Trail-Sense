package com.kylecorry.andromeda.sense.altitude

import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.read
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.time.Throttle
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.barometer.Barometer
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.andromeda.sense.location.filters.GPSPassThroughAltitudeFilter
import com.kylecorry.andromeda.sense.location.filters.GpsAltitudeEstimator
import com.kylecorry.andromeda.sense.location.hasFix
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Pressure
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

/**
 * A sensor that uses the GPS and barometer to calculate altitude.
 * @param gps The GPS sensor
 * @param barometer The barometer sensor
 * @param cache The cache to store calibration data
 * @param useContinuousCalibration True if the sensor should continuously calibrate using the GPS, otherwise it will only calibrate once an hour
 * @param gpsFilter The filter to use for the GPS altitude
 * @param recalibrationInterval The interval to recalibrate the sensor
 * @param useMSLAltitude True if the sensor should use the MSL altitude from the GPS, otherwise it will use the GPS altitude
 * @param logger Optional callback for debug information
 * @param maxGPSFixAge The maximum age of the GPS fix before it is considered too old to use
 * @param recalibrationTimeout The maximum time to wait for the GPS when recalibrating. If exceeded, the expired sea level pressure continues to be used and recalibration is retried later.
 */
class FusedAltimeter(
    private val gps: GpsSensor,
    private val barometer: Barometer,
    private val cache: Preferences,
    private val useContinuousCalibration: Boolean = true,
    private val gpsFilter: GpsAltitudeEstimator = GPSPassThroughAltitudeFilter(),
    private val recalibrationInterval: Duration = 1.hours,
    private val useMSLAltitude: Boolean = true,
    private val logger: ((String) -> Unit)? = null,
    private val maxGPSFixAge: Duration = 30.seconds,
    private val recalibrationTimeout: Duration = 30.seconds,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SharedSensor(), Altimeter {

    private var updateJob: Job? = null

    private var fusedAltitude: Float? = null

    override val altitude: Float
        get() = fusedAltitude ?: getGPSAltitude()

    override val hasValidReading: Boolean
        get() = fusedAltitude != null

    private var hasPendingGPSUpdate = false
    private val recalibrationThrottle = Throttle(RECALIBRATION_RETRY_INTERVAL.inWholeMilliseconds)

    override fun startImpl() {
        fusedAltitude = null
        hasPendingGPSUpdate = false
        gpsFilter.reset()
        if (useContinuousCalibration) {
            gps.start(this::onGPSUpdate)
        }
        barometer.start(this::onBarometerUpdate)
        updateJob = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive) {
                if (update()) {
                    setEventTimeFrom(barometer)
                    notifyListeners()
                }
                delay(UPDATE_FREQUENCY)
            }
        }
        updateJob?.start()
    }

    override fun stopImpl() {
        updateJob?.cancel()
        updateJob = null
        gps.stop(this::onGPSUpdate)
        barometer.stop(this::onBarometerUpdate)
    }

    private fun onBarometerUpdate(): Boolean {
        return true
    }

    private fun onGPSUpdate(): Boolean {
        gpsFilter.update(
            if (useMSLAltitude) gps.mslAltitude ?: gps.altitude else gps.altitude,
            gps.verticalAccuracy,
            gps.eventTimeElapsedNanos
        )
        hasPendingGPSUpdate = true
        return true
    }

    private suspend fun update(): Boolean {
        if (!barometer.hasValidReading || barometer.pressure == 0f) {
            // No barometer reading yet
            return false
        }

        val seaLevel = getLastSeaLevelPressure()

        // Sea level pressure is unknown, it needs to be calibrated
        if (seaLevel == null) {
            recalibrate(timeout = null)
            return false
        }

        // An expired sea level pressure continues to be used until recalibration succeeds
        if (isSeaLevelPressureExpired() && !recalibrationThrottle.isThrottled() && recalibrate(recalibrationTimeout)) {
            return false
        }

        // Calculate the barometric altitude
        val barometricAltitude =
            Meteorology.getAltitude(Pressure.hpa(barometer.pressure), seaLevel).value

        // At this point, the barometric altitude is available but the GPS altitude may not be
        // If the GPS has a reading, use the GPS altimeter (even if the gaussian filter hasn't converged)
        // Otherwise, use the barometric altitude
        val gpsAltitude = if (hasGPSFix()) {
            getGPSAltitude()
        } else {
            barometricAltitude
        }

        // Dynamically calculate the influence of the GPS using the altitude accuracy
        // A more accurate GPS reading will have a higher weight
        // If always on calibration is disabled, the GPS will not be used
        val gpsError = if (hasGPSFix()) {
            gpsFilter.accuracy ?: MAX_GPS_ERROR
        } else {
            MAX_GPS_ERROR
        }
        val gpsWeight = if (useContinuousCalibration && hasPendingGPSUpdate) {
            hasPendingGPSUpdate = false

            // The weight is higher for more accurate GPS readings
            val errorWeight =
                1 - Interpolation.map(gpsError, 0f, MAX_GPS_ERROR, MIN_ALPHA, MAX_ALPHA)
                    .coerceIn(MIN_ALPHA, MAX_ALPHA)

            // The weight increases over time (up to a maximum at MAX_TIME_FOR_WEIGHT hours)
            val timeSinceLastGPSUsed = getTimeSinceLastGPSUsed()?.inWholeSeconds?.toFloat() ?: 0f
            val timeWeight = Interpolation.map(
                timeSinceLastGPSUsed,
                0f,
                MAX_TIME_FOR_WEIGHT.inWholeSeconds.toFloat(),
                0f,
                MAX_ALPHA_TIME
            ).coerceIn(0f, MAX_ALPHA_TIME)

            // Simply add the weights together and restrict to the range
            (errorWeight + timeWeight).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Create the filter if it doesn't exist, the value will be overwritten so it doesn't matter
        val altitude = gpsWeight * gpsAltitude + (1 - gpsWeight) * barometricAltitude
        fusedAltitude = altitude

        // Update the current estimate of the sea level pressure
        setLastSeaLevelPressure(
            Meteorology.getSeaLevelPressure(
                Pressure.hpa(barometer.pressure),
                Distance.meters(altitude)
            ),
            isBaseline = false,
            wasGPSUsed = gpsWeight > 0
        )

        logger?.invoke(
                "ALT: ${DecimalFormatter.format(altitude, 2, true)}, " +
                        "GPS: ${DecimalFormatter.format(getGPSAltitude(), 2, true)}, " +
                        "BAR: ${DecimalFormatter.format(barometer.pressure, 2, true)}, " +
                        "SEA: ${DecimalFormatter.format(seaLevel.value, 2, true)}, " +
                        "ALPHA: ${DecimalFormatter.format(gpsWeight, 3, true)}, " +
                        "ERR: ${DecimalFormatter.format(gpsError, 2, true)}"
        )

        // When continuous calibration is enabled, ensure a GPS fix before notifying listeners
        return if (useContinuousCalibration) {
            gpsWeight > 0
        } else {
            true
        }
    }

    private fun getGPSAltitude(): Float {
        val altitude = gpsFilter.altitude
        return if (altitude == 0f && !hasFilteredGPSFix()) {
            if (useMSLAltitude) gps.mslAltitude ?: gps.altitude else gps.altitude
        } else {
            altitude
        }
    }

    private suspend fun recalibrate(timeout: Duration?): Boolean {
        if (barometer.pressure <= 0f) {
            return false
        }

        gpsFilter.reset()

        withTimeoutOrNull(timeout?.inWholeMilliseconds ?: Long.MAX_VALUE) {
            gps.read {
                onGPSUpdate()
                gpsFilter.hasValidReading
            }
        } ?: return false

        setLastSeaLevelPressure(
            Meteorology.getSeaLevelPressure(
                Pressure.hpa(barometer.pressure),
                Distance.meters(getGPSAltitude())
            ),
            isBaseline = true,
            wasGPSUsed = true
        )
        return true
    }

    private fun hasGPSFix(): Boolean {
        return gps.hasFix(maxGPSFixAge)
    }

    private fun hasFilteredGPSFix(): Boolean {
        return hasGPSFix() && gpsFilter.hasValidReading
    }

    private fun getLastSeaLevelPressure(): Pressure? {
        return cache.getFloat(LAST_SEA_LEVEL_PRESSURE_KEY)?.let { Pressure.hpa(it) }
    }

    private fun isSeaLevelPressureExpired(): Boolean {
        val time = cache.getInstant(LAST_SEA_LEVEL_PRESSURE_TIME_KEY) ?: return true
        val timeSinceReading = Clock.System.now() - time
        return timeSinceReading > recalibrationInterval || timeSinceReading.isNegative()
    }

    private fun getTimeSinceLastGPSUsed(): Duration? {
        val time = cache.getInstant(LAST_GPS_USED_TIME_KEY) ?: return null
        return Clock.System.now() - time
    }

    private fun setLastSeaLevelPressure(
        pressure: Pressure,
        isBaseline: Boolean,
        wasGPSUsed: Boolean
    ) {
        cache.putFloat(LAST_SEA_LEVEL_PRESSURE_KEY, pressure.value)

        // Only update the time if the gaussian GPS was used - this prevents only using the barometer over the long term
        if (isBaseline) {
            cache.putInstant(LAST_SEA_LEVEL_PRESSURE_TIME_KEY, Clock.System.now())
        }

        if (wasGPSUsed) {
            cache.putInstant(LAST_GPS_USED_TIME_KEY, Clock.System.now())
        }
    }

    companion object {
        private const val LAST_SEA_LEVEL_PRESSURE_KEY =
            "andromeda_fused_altimeter_last_sea_level_pressure"
        private const val LAST_SEA_LEVEL_PRESSURE_TIME_KEY =
            "andromeda_fused_altimeter_last_sea_level_pressure_time"
        private const val LAST_GPS_USED_TIME_KEY = "andromeda_fused_altimeter_last_gps_used_time"

        private const val MIN_ALPHA = 0.9f
        private const val MAX_ALPHA = 0.99f
        private const val MAX_ALPHA_TIME = 0.4f
        private val MAX_TIME_FOR_WEIGHT = 2.hours
        private const val MAX_GPS_ERROR = 5f
        private val UPDATE_FREQUENCY = 200.milliseconds
        private val RECALIBRATION_RETRY_INTERVAL = 5.minutes

        /**
         * Clear the cached calibration data
         * @param cache The cache to clear from
         */
        fun clearCachedCalibration(cache: Preferences) {
            cache.remove(LAST_SEA_LEVEL_PRESSURE_KEY)
            cache.remove(LAST_SEA_LEVEL_PRESSURE_TIME_KEY)
            cache.remove(LAST_GPS_USED_TIME_KEY)
        }
    }
}
