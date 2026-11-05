package com.kylecorry.trail_sense.tools.guide.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionUserGuide(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {
    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.UserGuide)
    }

    override fun onClick() {
        super.onClick()
        val activeTool = Tools.getTools(context).firstOrNull {
            it.isOpen(host.findNavigator().currentDestination?.id ?: 0)
        }
        if (activeTool?.guideId != null) {
            UserGuideUtils.showGuide(context, host.findNavigator(), activeTool.guideId)
        } else {
            UserGuideUtils.showGuide(context, host.findNavigator(), R.raw.guide_tool_tools)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().openTool(Tools.USER_GUIDE)
        return true
    }
}
