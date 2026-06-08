package com.kylecorry.trail_sense.tools.convert.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionConvert(btn: QuickActionButtonView, fragment: TrailSenseComposeDestinationFragment) :
    QuickActionButton(btn, fragment) {
    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_tool_distance_convert)
    }

    override fun onClick() {
        super.onClick()
        fragment.findNavController().openTool(Tools.CONVERT)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.CONVERT)
        return true
    }
}
