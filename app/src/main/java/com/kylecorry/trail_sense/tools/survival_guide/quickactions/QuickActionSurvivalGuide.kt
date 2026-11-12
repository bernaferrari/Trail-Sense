package com.kylecorry.trail_sense.tools.survival_guide.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.tools.survival_guide.ui.showSurvivalGuideQuickSearch

class QuickActionSurvivalGuide(btn: QuickActionButtonView, host: AndroidDestinationHost) :
    QuickActionButton(btn, host) {
    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.SurvivalGuide)
    }

    override fun onClick() {
        super.onClick()
        showSurvivalGuideQuickSearch(AppHost.from(host.context), host.navigator)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.SurvivalGuideList())
        return true
    }
}
