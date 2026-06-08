package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView

class QuickActionNone(button: QuickActionButtonView, fragment: TrailSenseComposeDestinationFragment) : QuickActionButton(
    button,
    fragment
) {
    override fun onCreate() {
        super.onCreate()
        button.isVisible = false
    }
}
