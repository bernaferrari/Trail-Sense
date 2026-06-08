package com.kylecorry.trail_sense.tools.clinometer.volumeactions

import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerLockController

class ClinometerLockVolumeAction(fragment: Fragment) : VolumeAction {

    private val clinometerController = fragment as? ClinometerLockController

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        clinometerController?.onTouchDown()
        return clinometerController != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        clinometerController?.onTouchUp()
        return clinometerController != null
    }
}
