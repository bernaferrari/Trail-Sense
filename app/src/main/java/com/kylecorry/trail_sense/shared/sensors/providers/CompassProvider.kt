package com.kylecorry.trail_sense.shared.sensors.providers

import com.kylecorry.trail_sense.shared.andromeda_temp.sense.magnetometer.AndroidMagnetometer
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.orientation.AndroidGyroscope

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.accelerometer.AndroidAccelerometer
import android.view.Surface
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.Sensors
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.accelerometer.GravitySensor
import com.kylecorry.andromeda.sense.accelerometer.LowPassAccelerometer
import com.kylecorry.andromeda.sense.compass.Compass
import com.kylecorry.andromeda.sense.compass.FilteredCompass
import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.compass.LegacyCompass
import com.kylecorry.andromeda.sense.magnetometer.LowPassMagnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.andromeda.sense.orientation.CustomGeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.CustomRotationSensor
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.orientation.GeomagneticRotationSensor
import com.kylecorry.andromeda.sense.orientation.GravityRotationSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.orientation.RotationSensor
import com.kylecorry.andromeda.sense.orientation.filter.FilteredOrientationSensor
import com.kylecorry.andromeda.sense.orientation.filter.LowPassOrientationSensorFilter
import com.kylecorry.sol.math.filters.MovingAverageFilter
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.settings.infrastructure.ICompassPreferences
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityCompassWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MagQualityOrientationWrapper
import com.kylecorry.trail_sense.shared.sensors.compass.MockCompass
import com.kylecorry.trail_sense.shared.sensors.compass.QuickRecalibrationOrientationSensor
import kotlin.math.pow

class CompassProvider(private val context: Context, private val prefs: ICompassPreferences) {

    fun get(sensorDelay: Int, existingOrientationSensor: OrientationSensor? = null): CompassSensor {
        val useTrueNorth = prefs.useTrueNorth

        var source = prefs.source

        // Handle if the available sources have changed (not likely)
        val allSources = getAvailableSources(context)

        // There were no compass sensors found
        if (allSources.isEmpty()) {
            return MockCompass()
        }

        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
        }

        val compass = when (source) {
            CompassSource.Orientation -> {
                FilteredCompass(
                    LegacyCompass(context, useTrueNorth, sensorDelay),
                    MovingAverageFilter((prefs.compassSmoothing * 4).coerceAtLeast(1))
                )
            }

            else -> {
                Compass(
                    existingOrientationSensor ?: getOrientationSensor(sensorDelay),
                    useTrueNorth,
                    surfaceRotation = getCompassSurfaceRotation(),
                    offset = -90f
                )
            }
        }

        return MagQualityCompassWrapper(
            compass,
            AndroidMagnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        )
    }

    private fun getBaseOrientationSensor(sensorDelay: Int): OrientationSensor {
        var source = prefs.source

        // Swap out the legacy orientation sensor for the rotation vector sensor
        if (source == CompassSource.Orientation) {
            source = CompassSource.RotationVector
        }

        val allSources = getAvailableSources(context)

        if (!allSources.contains(source)) {
            source = allSources.firstOrNull() ?: CompassSource.CustomMagnetometer
            if (source == CompassSource.Orientation) {
                source = CompassSource.CustomMagnetometer
            }
        }

        if (source == CompassSource.RotationVector) {
            return RotationSensor(context, sensorDelay)
        }

        if (source == CompassSource.GeomagneticRotationVector) {
            return GeomagneticRotationSensor(
                context,
                sensorDelay
            )
        }

        if (source == CompassSource.CustomRotationVector) {
            return getCustomRotationSensor(sensorDelay)
        }

        return getCustomGeomagneticRotationSensor(true, sensorDelay)
    }

    fun getOrientationSensor(sensorDelay: Int): OrientationSensor {
        val smoothing = prefs.compassSmoothing

        val baseOrientationSensor = getBaseOrientationSensor(sensorDelay)

        // Don't use quick recalibration for custom sensors
        val isCustomSensor = baseOrientationSensor is CustomGeomagneticRotationSensor ||
                baseOrientationSensor is CustomRotationSensor ||
                baseOrientationSensor is GravityRotationSensor

        val quickRecalibration =
            if (isCustomSensor) {
                baseOrientationSensor
            } else {
                QuickRecalibrationOrientationSensor(
                    getCustomGeomagneticRotationSensor(false, sensorDelay),
                    baseOrientationSensor,
                    1f,
                    45f
                )
            }

        // Smoothing isn't needed
        if (smoothing <= 1) {
            return MagQualityOrientationWrapper(
                quickRecalibration,
                AndroidMagnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
            )
        }

        val alpha = Interpolation.map((1 - smoothing / 100f).pow(2), 0f, 1f, 0.005f, 1f)

        return MagQualityOrientationWrapper(
            FilteredOrientationSensor(
                quickRecalibration,
                LowPassOrientationSensorFilter(alpha, true)
            ),
            AndroidMagnetometer(context, SensorManager.SENSOR_DELAY_NORMAL)
        )

    }

    private fun getCompassSurfaceRotation(): Int {
        return when (MainActivity.deviceSurfaceRotation) {
            Surface.ROTATION_0 -> Surface.ROTATION_90
            Surface.ROTATION_90 -> Surface.ROTATION_180
            Surface.ROTATION_180 -> Surface.ROTATION_270
            Surface.ROTATION_270 -> Surface.ROTATION_0
            else -> Surface.ROTATION_90
        }
    }

    private fun getCustomGeomagneticRotationSensor(
        useGyroIfAvailable: Boolean,
        sensorDelay: Int
    ): OrientationSensor {

        val accelerometer: Accelerometer = if (useGyroIfAvailable && Sensors.hasGravity(context)) {
            GravitySensor(context, sensorDelay)
        } else {
            LowPassAccelerometer(AndroidAccelerometer(context, sensorDelay), ACCELEROMETER_LOW_PASS)
        }

        if (!Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
            return GravityRotationSensor(accelerometer)
        }

        val magnetometer = LowPassMagnetometer(AndroidMagnetometer(context, sensorDelay), MAGNETOMETER_LOW_PASS)

        return CustomGeomagneticRotationSensor(
            magnetometer,
            accelerometer,
            onlyUseMagnetometerQuality = true
        )
    }

    private fun getCustomRotationSensor(sensorDelay: Int): OrientationSensor {
        val magnetometer = LowPassMagnetometer(AndroidMagnetometer(context, sensorDelay), MAGNETOMETER_LOW_PASS)
        val accelerometer = LowPassAccelerometer(AndroidAccelerometer(context, sensorDelay), ACCELEROMETER_LOW_PASS)
        val gyro = AndroidGyroscope(context, sensorDelay)

        return CustomRotationSensor(
            magnetometer, accelerometer, gyro,
            scope = sensorScope,
            validMagnetometerMagnitudes = 20f..65f,
            validAccelerometerMagnitudes = 4f..20f,
            onlyUseMagnetometerQuality = true
        )
    }

    companion object {

        // Long-lived scope for fused sensors; their jobs are started and cancelled with the sensor itself
        private val sensorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        const val MAGNETOMETER_LOW_PASS = 0.3f
        const val ACCELEROMETER_LOW_PASS = 0.1f

        /**
         * Returns the available compass sources in order of quality
         */
        fun getAvailableSources(context: Context): List<CompassSource> {
            val sources = mutableListOf<CompassSource>()

            if (Sensors.hasSensor(context, Sensor.TYPE_GYROSCOPE) &&
                Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)
            ) {
                sources.add(CompassSource.CustomRotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_ROTATION_VECTOR)) {
                sources.add(CompassSource.RotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)) {
                sources.add(CompassSource.GeomagneticRotationVector)
            }

            if (Sensors.hasSensor(context, Sensor.TYPE_MAGNETIC_FIELD)) {
                sources.add(CompassSource.CustomMagnetometer)
            }

            @Suppress("DEPRECATION")
            if (Sensors.hasSensor(context, Sensor.TYPE_ORIENTATION)) {
                sources.add(CompassSource.Orientation)
            }

            return sources
        }
    }

}
