package com.kylecorry.trail_sense.settings.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionSettings(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_settings)
    }

    override fun onClick() {
        super.onClick()
        val activeTool = Tools.getTools(context).firstOrNull {
            it.isOpen(host.findNavController().currentDestination?.id ?: 0)
        }
        if (activeTool?.settingsNavAction != null) {
            host.findNavController().navigateWithAnimation(activeTool.settingsNavAction)
        } else {
            host.findNavController().openTool(Tools.SETTINGS)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavController().openTool(Tools.SETTINGS)
        return true
    }
}
