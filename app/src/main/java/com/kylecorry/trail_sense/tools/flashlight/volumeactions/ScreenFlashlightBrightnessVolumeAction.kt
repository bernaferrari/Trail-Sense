package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenFlashlightController

class ScreenFlashlightBrightnessVolumeAction(
    private val fragment: Fragment
) : VolumeAction {

    private val screenFlashlightController = fragment as? ScreenFlashlightController

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        screenFlashlightController?.handleVolumeButtonPress(isVolumeUp = isUpButton)
        return screenFlashlightController != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return screenFlashlightController != null
    }
}
