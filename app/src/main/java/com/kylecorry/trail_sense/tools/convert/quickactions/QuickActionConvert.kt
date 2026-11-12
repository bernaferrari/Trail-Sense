package com.kylecorry.trail_sense.tools.convert.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.tools.convert.ui.showConvertQuickSheet
import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionConvert(btn: QuickActionButtonView, host: AndroidDestinationHost) :
    QuickActionButton(btn, host) {
    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ToolDistanceConvert)
    }

    override fun onClick() {
        super.onClick()
        showConvertQuickSheet(AppHost.from(host.context))
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.Convert)
        return true
    }
}
