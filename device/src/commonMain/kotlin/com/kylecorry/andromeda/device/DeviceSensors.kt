package com.kylecorry.andromeda.device

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
}
