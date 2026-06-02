package com.kylecorry.trail_sense.tools.flashlight.volumeactions

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolScreenFlashlight

class ScreenFlashlightBrightnessVolumeAction(
    private val fragment: Fragment
) : VolumeAction {

    private val screenFlashlightFragment = fragment as? FragmentToolScreenFlashlight

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        screenFlashlightFragment?.handleVolumeButtonPress(isVolumeUp = isUpButton)
        return screenFlashlightFragment != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return screenFlashlightFragment != null
    }
}
