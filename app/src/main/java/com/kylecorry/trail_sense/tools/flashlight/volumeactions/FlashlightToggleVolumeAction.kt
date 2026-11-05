package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import android.content.Context
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightController
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightToggle

/**
 * Toggles the flashlight with a volume button. The Flashlight screen offers its own [FlashlightController] (it turns
 * on the mode picked there); anywhere else the torch toggles.
 */
class FlashlightToggleVolumeAction(
    private val context: Context,
    private val flashlightController: FlashlightController?
) : VolumeAction {

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        val controller = flashlightController ?: FlashlightToggle(context)
        controller.toggle()
        return true
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        // Do nothing
        return true
    }
}
