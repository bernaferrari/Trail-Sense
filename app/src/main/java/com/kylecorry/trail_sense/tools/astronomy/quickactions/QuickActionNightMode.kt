package com.kylecorry.trail_sense.tools.astronomy.quickactions

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences

class QuickActionNightMode(btn: QuickActionButtonView, fragment: TrailSenseDestinationHost) :
    QuickActionButton(btn, fragment) {

    private var isSwitching = false
    private val prefs by lazy { UserPreferences(context) }


    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_astronomy)
        setState(prefs.theme == UserPreferences.Theme.Night)
    }

    override fun onClick() {
        super.onClick()
        if (isSwitching) {
            return
        }
        isSwitching = true

        prefs.theme = if (prefs.theme == UserPreferences.Theme.Night) {
            prefs.lastTheme
        } else {
            prefs.lastTheme = prefs.theme
            UserPreferences.Theme.Night
        }

        fragment.reloadTheme()
    }
}
