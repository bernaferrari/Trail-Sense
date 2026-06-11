package com.kylecorry.trail_sense.tools.tools.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton

class QuickActionToolWidgets(button: QuickActionButtonView, host: TrailSenseDestinationHost) : QuickActionButton(
    button,
    host
) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_summary)
    }

    override fun onClick() {
        super.onClick()
        host.openWidgets()
    }

}
