package com.kylecorry.trail_sense.tools.guide.quickactions

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
        setIcon(R.drawable.user_guide)
    }

    override fun onClick() {
        super.onClick()
        val activeTool = Tools.getTools(context).firstOrNull {
            it.isOpen(host.findNavController().currentDestination?.id ?: 0)
        }
        if (activeTool?.guideId != null) {
            UserGuideUtils.showGuide(context, host.findNavController(), activeTool.guideId)
        } else {
            UserGuideUtils.showGuide(context, host.findNavController(), R.raw.guide_tool_tools)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().openTool(Tools.USER_GUIDE)
        return true
    }
}
