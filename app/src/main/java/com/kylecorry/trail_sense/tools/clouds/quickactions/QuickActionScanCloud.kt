package com.kylecorry.trail_sense.tools.clouds.quickactions

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.os.Bundle
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionScanCloud(button: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(button, host) {

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.CloudScanner)
    }

    override fun onClick() {
        super.onClick()
        host.navigator.navigate(Route.Clouds(openScanner = true))
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(
            Route.Clouds()
        )
        return true
    }


}
