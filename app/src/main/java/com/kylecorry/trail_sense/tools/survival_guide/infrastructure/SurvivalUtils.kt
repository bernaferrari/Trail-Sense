package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.trail_sense.tools.survival_guide.ui.SurvivalGuideBottomSheet

object SurvivalUtils {

    fun showSurvival(fragment: Fragment): BottomSheetDialog {
        return SurvivalGuideBottomSheet(fragment).show()
    }
}
