package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import android.content.Context
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightController

class FlashlightToggleVolumeAction(
    private val context: Context,
    private val flashlightController: FlashlightController?
) : VolumeAction {

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        if (flashlightController == null) {
            val flashlight = FlashlightSubsystem.getInstance(context)
            flashlight.toggle()
        } else {
            flashlightController.toggle()
        }
        return true
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        // Do nothing
        return true
    }
}
