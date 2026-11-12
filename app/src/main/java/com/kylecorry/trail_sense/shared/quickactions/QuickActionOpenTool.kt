package com.kylecorry.trail_sense.shared.quickactions

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionOpenTool(
    button: QuickActionButtonView,
    host: AndroidDestinationHost,
    private val route: Route,
    private val icon: ImageVector
) :
    QuickActionButton(button, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(icon)
    }

    override fun onClick() {
        super.onClick()
        host.navigator.navigate(route)
    }

}
