package com.kylecorry.trail_sense.tools.flashlight.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionScreenFlashlight(btn: QuickActionButtonView, host: AndroidDestinationHost) :
    QuickActionButton(btn, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ScreenFlashlight)
    }

    override fun onClick() {
        super.onClick()
        host.navigator.navigate(Route.ScreenFlashlight)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.Flashlight)
        return true
    }
}
