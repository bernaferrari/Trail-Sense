package com.kylecorry.andromeda.device

import com.ksensor.core.SensorConfig
import com.ksensor.core.PlatformType
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import com.ksensor.plugins.sensors.environment.createEnvironmentPlugin
import com.ksensor.plugins.sensors.motion.createMotionPlugin
import com.ksensor.plugins.sensors.positioning.createPositioningPlugin
import com.ksensor.plugins.states.system.createSystemPlugin

/** Cross-platform device observations. Each plugin exposes its readings as Kotlin flows. */
class DeviceSensors {
    val motion by lazy { createMotionPlugin() }
    val environment by lazy { createEnvironmentPlugin() }
    val positioning by lazy { createPositioningPlugin() }
    val system by lazy { createSystemPlugin() }

    /** A barometer implementing Andromeda's shared sensor contract. */
    fun barometer(scope: CoroutineScope, config: SensorConfig = SensorConfig.Default) =
        FlowBarometer(pressure(config), scope)

    /** Atmospheric pressure in hectopascals. */
    fun pressure(config: SensorConfig = SensorConfig.Default) = environment.barometer(config).map { reading ->
        val factor = if (reading.platform == PlatformType.iOS) 10f else 1f
        Reading(reading.data.pressure * factor, Instant.fromEpochMilliseconds(reading.timestamp))
    }
}
