package com.kylecorry.andromeda.device

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

    fun location(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<LocationReading>> =
        backend.location(interval, accuracy)
}

internal interface DeviceBackend {
    fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>>
}

internal expect fun createDeviceBackend(): DeviceBackend
