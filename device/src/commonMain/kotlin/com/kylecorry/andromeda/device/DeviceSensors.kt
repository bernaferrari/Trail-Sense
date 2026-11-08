package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class SensorAccuracy { PowerSave, Balanced, HighPrecision }

data class LocationReading(val coordinate: Coordinate, val altitudeMeters: Double?)

/** Cross-platform device observations. */
class DeviceSensors {
    internal val backend = createDeviceBackend()

    fun barometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowBarometer(pressure(interval, accuracy), scope)

    /** Atmospheric pressure in hectopascals. */
    fun pressure(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.pressure(interval, accuracy)

    fun accelerometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowAccelerometer(acceleration(interval, accuracy), scope)

    /** Acceleration including gravity, in meters per second squared. */
    fun acceleration(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.acceleration(interval, accuracy)

    fun location(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<LocationReading>> =
        backend.location(interval, accuracy)
}

internal interface DeviceBackend {
    fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>>
    fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
}

internal expect fun createDeviceBackend(): DeviceBackend
