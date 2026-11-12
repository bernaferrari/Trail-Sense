package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView

class QuickActionNone(button: QuickActionButtonView, host: AndroidDestinationHost) : QuickActionButton(
    button,
    host
) {
    override fun onCreate() {
        super.onCreate()
        button.isVisible = false
    }
}
