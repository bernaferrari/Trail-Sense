package com.kylecorry.andromeda.device

import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

internal actual fun createDeviceBackend(): DeviceBackend = object : DeviceBackend {
    override fun battery(): Flow<Reading<BatteryReading>> = emptyFlow()
    override fun angularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun magneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>> = emptyFlow()
    override fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
}
