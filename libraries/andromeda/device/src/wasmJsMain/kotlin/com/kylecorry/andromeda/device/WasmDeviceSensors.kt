package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

internal actual fun createDeviceBackend(): DeviceBackend = object : DeviceBackend {
    override fun attitude(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<DeviceAttitude>> = emptyFlow()
    override fun gravity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun linearAcceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun magneticHeading(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun illuminance(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun ambientTemperature(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun relativeHumidity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun steps(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Int>> = emptyFlow()
    override fun proximity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Boolean>> = emptyFlow()
    override fun battery(): Flow<Reading<BatteryReading>> = emptyFlow()
    override fun angularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun magneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>> = emptyFlow()
    override fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
}

internal actual fun platformSatelliteStatus(): Flow<Reading<List<Satellite>>> = emptyFlow()
