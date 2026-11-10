package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import kotlinx.coroutines.flow.StateFlow


interface IFlashlightSubsystem {
    val mode: StateFlow<FlashlightMode>
    var selectedMode: FlashlightMode
    val brightnessLevels: Int
    fun setBrightness(brightness: Float)
    fun toggle()
    fun set(mode: FlashlightMode)
    fun getMode(): FlashlightMode
    fun isAvailable(): Boolean
}
