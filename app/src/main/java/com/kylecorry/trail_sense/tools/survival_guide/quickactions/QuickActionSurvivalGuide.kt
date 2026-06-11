package com.kylecorry.trail_sense.tools.survival_guide.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionSurvivalGuide(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {
    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.survival_guide)
    }

    override fun onClick() {
        super.onClick()
        host.findNavController().openTool(Tools.SURVIVAL_GUIDE)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().openTool(Tools.SURVIVAL_GUIDE)
        return true
    }
}
