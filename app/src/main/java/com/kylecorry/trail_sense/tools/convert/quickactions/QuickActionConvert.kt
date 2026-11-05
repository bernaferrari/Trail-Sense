package com.kylecorry.trail_sense.tools.convert.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionConvert(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {
    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ToolDistanceConvert)
    }

    override fun onClick() {
        super.onClick()
        host.findNavigator().openTool(Tools.CONVERT)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().openTool(Tools.CONVERT)
        return true
    }
}
