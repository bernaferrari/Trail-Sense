package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.ui.FlashlightController

class FlashlightToggleVolumeAction(
    private val fragment: Fragment
) : VolumeAction {

    private val flashlightController = fragment as? FlashlightController

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        if (flashlightController == null) {
            val flashlight = FlashlightSubsystem.getInstance(fragment.requireContext())
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
