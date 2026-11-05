package com.kylecorry.trail_sense.settings.quickactions

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionSettings(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.Settings)
    }

    override fun onClick() {
        super.onClick()
        val activeTool = Tools.getTools(context).firstOrNull {
            it.isOpen(host.findNavigator().currentDestination?.id ?: 0)
        }
        if (activeTool?.settingsNavAction != null) {
            host.findNavigator().navigate(activeTool.settingsNavAction)
        } else {
            host.findNavigator().openTool(Tools.SETTINGS)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().openTool(Tools.SETTINGS)
        return true
    }
}
