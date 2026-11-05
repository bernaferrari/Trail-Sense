package com.kylecorry.trail_sense.tools.flashlight.ui

import android.content.Context
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

interface FlashlightController {
    fun toggle()
}

/** Turns the flashlight off when it is on, otherwise on in [selectedMode], with a click. */
class FlashlightToggle(
    context: Context,
    var selectedMode: FlashlightMode = FlashlightMode.Torch
) : FlashlightController {
    private val flashlight = FlashlightSubsystem.getInstance(context)
    private val haptics = HapticSubsystem.getInstance(context)

    override fun toggle() {
        haptics.click()
        if (flashlight.getMode() != FlashlightMode.Off) {
            flashlight.set(FlashlightMode.Off)
        } else {
            flashlight.set(selectedMode)
        }
    }
}
