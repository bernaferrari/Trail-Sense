package com.kylecorry.andromeda.device

import com.ksensor.core.SensorConfig
import com.ksensor.core.model.StateData
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.ksensor.plugins.sensors.environment.createEnvironmentPlugin
import com.ksensor.plugins.sensors.motion.createMotionPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.ksensor.plugins.states.system.createSystemPlugin
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Instant

internal class MobileDeviceBackend : DeviceBackend {
    val motion by lazy { createMotionPlugin() }
    val environment by lazy { createEnvironmentPlugin() }
    val positioning by lazy { createPositioningPlugin() }
    val system by lazy { createSystemPlugin() }

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
        val details = platformBatteryDetails(state.levelPercent)
        Reading(BatteryReading(state.levelPercent, charging, health, state.temperatureC,
            details.voltageVolts, details.currentMilliamps, details.chargeMilliampHours,
            details.fullChargeMilliampHours, details.chargingMethod), Instant.fromEpochMilliseconds(reading.timestamp))
    }

    override fun gravity(interval: Duration, accuracy: SensorAccuracy) = platformGravity(interval)
    override fun linearAcceleration(interval: Duration, accuracy: SensorAccuracy) = platformLinearAcceleration(interval)
    override fun magneticHeading(interval: Duration, accuracy: SensorAccuracy) = platformMagneticHeading(interval)
    override fun ambientTemperature(interval: Duration, accuracy: SensorAccuracy) = platformAmbientTemperature(interval)
    override fun relativeHumidity(interval: Duration, accuracy: SensorAccuracy) = platformRelativeHumidity(interval)

    override fun illuminance(interval: Duration, accuracy: SensorAccuracy) =
        environment.light(config(interval, accuracy)).map { Reading(it.data.illuminance, Instant.fromEpochMilliseconds(it.timestamp)) }

    override fun steps(interval: Duration, accuracy: SensorAccuracy) =
        motion.stepCounter(config(interval, accuracy)).map { Reading(it.data.steps, Instant.fromEpochMilliseconds(it.timestamp)) }

    override fun proximity(interval: Duration, accuracy: SensorAccuracy) =
        environment.proximity(config(interval, accuracy)).map { Reading(it.data.isNear, Instant.fromEpochMilliseconds(it.timestamp)) }

    override fun pressure(interval: Duration, accuracy: SensorAccuracy) =
        platformPressure(interval, accuracy)

    override fun angularVelocity(interval: Duration, accuracy: SensorAccuracy) =
        platformAngularVelocity(interval, accuracy)

    override fun magneticField(interval: Duration, accuracy: SensorAccuracy) =
        platformMagneticField(interval, accuracy)

    override fun acceleration(interval: Duration, accuracy: SensorAccuracy) =
        platformAcceleration(interval, accuracy)

    override fun location(interval: Duration, accuracy: SensorAccuracy) =
        platformLocation(interval, accuracy)

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

internal expect fun platformLocation(interval: Duration, accuracy: SensorAccuracy): kotlinx.coroutines.flow.Flow<Reading<LocationReading>>
internal expect fun platformPressure(interval: Duration, accuracy: SensorAccuracy): kotlinx.coroutines.flow.Flow<Reading<Float>>
internal expect fun platformAcceleration(interval: Duration, accuracy: SensorAccuracy): kotlinx.coroutines.flow.Flow<Reading<Vector3>>
internal expect fun platformAngularVelocity(interval: Duration, accuracy: SensorAccuracy): kotlinx.coroutines.flow.Flow<Reading<Vector3>>
internal expect fun platformMagneticField(interval: Duration, accuracy: SensorAccuracy): kotlinx.coroutines.flow.Flow<Reading<Vector3>>

/** Access to platform sensor plugins on Android and iOS. */
val DeviceSensors.motion get() = (backend as MobileDeviceBackend).motion
val DeviceSensors.environment get() = (backend as MobileDeviceBackend).environment
val DeviceSensors.positioning get() = (backend as MobileDeviceBackend).positioning
val DeviceSensors.system get() = (backend as MobileDeviceBackend).system

internal expect fun platformAmbientTemperature(interval: Duration): kotlinx.coroutines.flow.Flow<Reading<Float>>
internal expect fun platformRelativeHumidity(interval: Duration): kotlinx.coroutines.flow.Flow<Reading<Float>>

internal data class BatteryDetails(
    val voltageVolts: Float? = null,
    val currentMilliamps: Float? = null,
    val chargeMilliampHours: Float? = null,
    val fullChargeMilliampHours: Float? = null,
    val chargingMethod: com.kylecorry.andromeda.battery.BatteryChargingMethod? = null,
)
internal expect fun platformBatteryDetails(percent: Int?): BatteryDetails

internal expect fun platformMagneticHeading(interval: Duration): kotlinx.coroutines.flow.Flow<Reading<Float>>

internal expect fun platformGravity(interval: Duration): kotlinx.coroutines.flow.Flow<Reading<Vector3>>
internal expect fun platformLinearAcceleration(interval: Duration): kotlinx.coroutines.flow.Flow<Reading<Vector3>>
