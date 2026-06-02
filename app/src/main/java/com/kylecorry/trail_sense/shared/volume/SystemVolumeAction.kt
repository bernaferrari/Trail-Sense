package com.kylecorry.trail_sense.shared.volume

import androidx.fragment.app.Fragment

class SystemVolumeAction(fragment: Fragment) : VolumeAction {
    override fun onButtonPress(isUpButton: Boolean): Boolean {
        return false
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return false
    }
}
