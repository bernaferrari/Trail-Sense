package com.kylecorry.andromeda.device

import com.ksensor.core.PlatformType
import com.ksensor.core.SensorConfig
import com.ksensor.plugins.sensors.environment.createEnvironmentPlugin
import com.ksensor.plugins.sensors.motion.createMotionPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.ksensor.plugins.states.system.createSystemPlugin
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.Instant

internal class MobileDeviceBackend : DeviceBackend {
    val motion by lazy { createMotionPlugin() }
    val environment by lazy { createEnvironmentPlugin() }
    val positioning by lazy { createPositioningPlugin() }
    val system by lazy { createSystemPlugin() }

    override fun pressure(interval: Duration) = environment.barometer(SensorConfig(intervalMs = interval.inWholeMilliseconds)).map { reading ->
        val factor = if (reading.platform == PlatformType.iOS) 10f else 1f
        Reading(reading.data.pressure * factor, Instant.fromEpochMilliseconds(reading.timestamp))
    }
}

internal actual fun createDeviceBackend(): DeviceBackend = MobileDeviceBackend()

/** Access to platform sensor plugins on Android and iOS. */
val DeviceSensors.motion get() = (backend as MobileDeviceBackend).motion
val DeviceSensors.environment get() = (backend as MobileDeviceBackend).environment
val DeviceSensors.positioning get() = (backend as MobileDeviceBackend).positioning
val DeviceSensors.system get() = (backend as MobileDeviceBackend).system
