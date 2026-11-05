package com.kylecorry.trail_sense.settings.quickactions

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
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
            it.isOpen(host.navigator.currentRoute)
        }
        if (activeTool?.settingsRoute != null) {
            host.navigator.navigate(activeTool.settingsRoute)
        } else {
            host.navigator.navigate(Route.Settings)
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.Settings)
        return true
    }
}
