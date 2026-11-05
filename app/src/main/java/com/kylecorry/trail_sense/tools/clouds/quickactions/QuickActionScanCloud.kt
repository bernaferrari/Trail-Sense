package com.kylecorry.trail_sense.tools.clouds.quickactions

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
        host.findNavigator().navigate(
            R.id.cloudFragment,
            Bundle().apply {
                putBoolean("open_scanner", true)
            },
        )
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().navigate(
            R.id.cloudFragment
        )
        return true
    }


}
