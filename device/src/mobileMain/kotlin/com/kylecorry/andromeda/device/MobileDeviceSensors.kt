package com.kylecorry.andromeda.device

import com.ksensor.core.PlatformType
import com.ksensor.core.SensorConfig
import com.ksensor.core.model.StateData
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.ksensor.plugins.sensors.environment.createEnvironmentPlugin
import com.ksensor.plugins.sensors.motion.createMotionPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.ksensor.plugins.states.system.createSystemPlugin
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration
import kotlin.time.Instant

internal class MobileDeviceBackend : DeviceBackend {
    val motion by lazy { createMotionPlugin() }
    val environment by lazy { createEnvironmentPlugin() }
    val positioning by lazy { createPositioningPlugin() }
    val system by lazy { createSystemPlugin() }
    private val accelerationFactor by lazy { accelerationScale() }

    override fun battery() = system.battery().observe().map { reading ->
        val state = reading.data
        val charging = when (state.chargingState) {
            StateData.BatteryStatus.ChargingState.UNKNOWN -> BatteryChargingStatus.Unknown
            StateData.BatteryStatus.ChargingState.DISCHARGING -> BatteryChargingStatus.Discharging
            StateData.BatteryStatus.ChargingState.CHARGING -> BatteryChargingStatus.Charging
            StateData.BatteryStatus.ChargingState.FULL -> BatteryChargingStatus.Full
        }
        val health = when (state.health) {
            null -> null
            StateData.BatteryStatus.BatteryHealth.GOOD -> BatteryHealth.Good
            StateData.BatteryStatus.BatteryHealth.OVERHEAT -> BatteryHealth.Overheat
            StateData.BatteryStatus.BatteryHealth.DEAD -> BatteryHealth.Dead
            StateData.BatteryStatus.BatteryHealth.OVER_VOLTAGE -> BatteryHealth.OverVoltage
            StateData.BatteryStatus.BatteryHealth.COLD -> BatteryHealth.Cold
            else -> BatteryHealth.Unknown
        }
        Reading(BatteryReading(state.levelPercent, charging, health, state.temperatureC), Instant.fromEpochMilliseconds(reading.timestamp))
    }

    override fun pressure(interval: Duration, accuracy: SensorAccuracy) = environment.barometer(config(interval, accuracy)).map { reading ->
        val factor = if (reading.platform == PlatformType.iOS) 10f else 1f
        Reading(reading.data.pressure * factor, Instant.fromEpochMilliseconds(reading.timestamp))
    }

    override fun angularVelocity(interval: Duration, accuracy: SensorAccuracy) = motion.gyroscope(config(interval, accuracy)).map { reading ->
        val value = reading.data.values
        val scale = (180.0 / kotlin.math.PI).toFloat()
        Reading(Vector3(value.x * scale, value.y * scale, value.z * scale), Instant.fromEpochMilliseconds(reading.timestamp))
    }

    override fun magneticField(interval: Duration, accuracy: SensorAccuracy) = positioning.magnetometer(config(interval, accuracy)).map { reading ->
        val value = reading.data.values
        Reading(Vector3(value.x, value.y, value.z), Instant.fromEpochMilliseconds(reading.timestamp))
    }

    override fun acceleration(interval: Duration, accuracy: SensorAccuracy) = motion.accelerometer(config(interval, accuracy)).map { reading ->
        val scale = accelerationFactor
        val value = reading.data.values
        Reading(Vector3(value.x * scale, value.y * scale, value.z * scale), Instant.fromEpochMilliseconds(reading.timestamp))
    }

    override fun location(interval: Duration, accuracy: SensorAccuracy) = positioning.location(config(interval, accuracy)).mapNotNull { reading ->
        val latitude = reading.data.latitude ?: return@mapNotNull null
        val longitude = reading.data.longitude ?: return@mapNotNull null
        Reading(LocationReading(Coordinate(latitude, longitude), reading.data.altitude), Instant.fromEpochMilliseconds(reading.timestamp))
    }

    private fun config(interval: Duration, accuracy: SensorAccuracy) = SensorConfig(
        intervalMs = interval.inWholeMilliseconds,
        accuracy = when (accuracy) {
            SensorAccuracy.PowerSave -> SensorConfig.Accuracy.POWER_SAVE
            SensorAccuracy.Balanced -> SensorConfig.Accuracy.BALANCED
            SensorAccuracy.HighPrecision -> SensorConfig.Accuracy.HIGH_PRECISION
        }
    )
}

internal actual fun createDeviceBackend(): DeviceBackend = MobileDeviceBackend()

internal expect fun accelerationScale(): Float

/** Access to platform sensor plugins on Android and iOS. */
val DeviceSensors.motion get() = (backend as MobileDeviceBackend).motion
val DeviceSensors.environment get() = (backend as MobileDeviceBackend).environment
val DeviceSensors.positioning get() = (backend as MobileDeviceBackend).positioning
val DeviceSensors.system get() = (backend as MobileDeviceBackend).system
