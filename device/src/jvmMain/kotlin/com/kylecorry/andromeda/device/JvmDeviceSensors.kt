package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import oshi.SystemInfo
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

internal actual fun createDeviceBackend(): DeviceBackend = object : DeviceBackend {
    override fun battery(): Flow<Reading<BatteryReading>> = flow {
        val hardware = SystemInfo().hardware
        while (currentCoroutineContext().isActive) {
            val battery = hardware.powerSources.firstOrNull()
            val fraction = battery?.remainingCapacityPercent
            val percent = fraction?.takeIf { it in 0.0..1.0 }?.let { (it * 100).roundToInt() }
            val status = when {
                battery == null -> BatteryChargingStatus.Unknown
                battery.isCharging -> BatteryChargingStatus.Charging
                battery.isDischarging -> BatteryChargingStatus.Discharging
                percent == 100 -> BatteryChargingStatus.Full
                battery.isPowerOnLine -> BatteryChargingStatus.NotCharging
                else -> BatteryChargingStatus.Unknown
            }
            emit(Reading(BatteryReading(percent, status, null, null,
                voltageVolts = battery?.voltage?.takeIf { it > 0 }?.toFloat(),
                currentMilliamps = battery?.amperage?.takeIf { it != 0.0 }?.toFloat()), Clock.System.now()))
            delay(30_000)
        }
    }.flowOn(Dispatchers.IO)
    override fun magneticHeading(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun ambientTemperature(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun relativeHumidity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun illuminance(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun steps(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Int>> = emptyFlow()
    override fun proximity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Boolean>> = emptyFlow()
    override fun angularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun magneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>> = emptyFlow()
    override fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
}
