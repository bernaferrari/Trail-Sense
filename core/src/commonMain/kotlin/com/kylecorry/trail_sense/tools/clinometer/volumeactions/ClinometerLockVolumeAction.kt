package com.kylecorry.trail_sense.tools.clinometer.volumeactions

import com.kylecorry.trail_sense.shared.volume.VolumeAction
import com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerLockController

class ClinometerLockVolumeAction(
    private val clinometerController: ClinometerLockController?
) : VolumeAction {

    override fun onButtonPress(isUpButton: Boolean): Boolean {
        clinometerController?.onTouchDown()
        return clinometerController != null
    }

    override fun onButtonRelease(isUpButton: Boolean): Boolean {
        clinometerController?.onTouchUp()
        return clinometerController != null
    }
}
