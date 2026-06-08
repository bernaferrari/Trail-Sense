package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.ui.ScreenFlashlightController

class ScreenFlashlightBrightnessVolumeAction(
    private val screenFlashlightController: ScreenFlashlightController?
) : VolumeAction {

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        screenFlashlightController?.handleVolumeButtonPress(isVolumeUp = isUpButton)
        return screenFlashlightController != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return screenFlashlightController != null
    }
}
