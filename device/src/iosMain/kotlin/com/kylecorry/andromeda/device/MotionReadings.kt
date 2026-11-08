package com.kylecorry.andromeda.device

import com.ksensor.core.SensorConfig
import com.ksensor.plugins.sensors.motion.createMotionPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Instant

internal actual fun platformAcceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    createMotionPlugin().accelerometer(config(interval, accuracy)).map { reading ->
        val value = reading.data.values
        // Core Motion reports g with the opposite sign to Android's acceleration convention.
        Reading(Vector3(value.x * -9.80665f, value.y * -9.80665f, value.z * -9.80665f), Instant.fromEpochMilliseconds(reading.timestamp))
    }

internal actual fun platformAngularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    createMotionPlugin().gyroscope(config(interval, accuracy)).map { reading ->
        val value = reading.data.values
        val scale = (180.0 / kotlin.math.PI).toFloat()
        Reading(Vector3(value.x * scale, value.y * scale, value.z * scale), Instant.fromEpochMilliseconds(reading.timestamp))
    }

internal actual fun platformMagneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    createPositioningPlugin().magnetometer(config(interval, accuracy)).map { reading ->
        val value = reading.data.values
        Reading(Vector3(value.x, value.y, value.z), Instant.fromEpochMilliseconds(reading.timestamp))
    }

private fun config(interval: Duration, accuracy: SensorAccuracy) = SensorConfig(
    intervalMs = interval.inWholeMilliseconds,
    accuracy = when (accuracy) {
        SensorAccuracy.PowerSave -> SensorConfig.Accuracy.POWER_SAVE
        SensorAccuracy.Balanced -> SensorConfig.Accuracy.BALANCED
        SensorAccuracy.HighPrecision -> SensorConfig.Accuracy.HIGH_PRECISION
    }
)
