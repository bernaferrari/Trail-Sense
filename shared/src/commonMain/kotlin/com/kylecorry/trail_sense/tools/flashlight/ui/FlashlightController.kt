package com.kylecorry.trail_sense.tools.flashlight.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.haptics.IHapticSubsystem
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.IFlashlightSubsystem

interface FlashlightController {
    fun toggle()
}

/** Turns the flashlight off when it is on, otherwise on in [selectedMode], with a click. */
class FlashlightToggle(
    var selectedMode: FlashlightMode = FlashlightMode.Torch
) : FlashlightController {
    private val flashlight = getAppService<IFlashlightSubsystem>()
    private val haptics = getAppService<IHapticSubsystem>()

    override fun toggle() {
        haptics.click()
        if (flashlight.getMode() != FlashlightMode.Off) {
            flashlight.set(FlashlightMode.Off)
        } else {
            flashlight.set(selectedMode)
        }
    }
}
