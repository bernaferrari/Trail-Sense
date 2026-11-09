package com.kylecorry.trail_sense.shared.volume

class SystemVolumeAction : VolumeAction {
    override fun onButtonPress(isUpButton: Boolean): Boolean {
        return false
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        return false
    }
}
