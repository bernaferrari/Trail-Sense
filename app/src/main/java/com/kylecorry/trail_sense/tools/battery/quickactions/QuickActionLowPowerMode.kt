package com.kylecorry.trail_sense.tools.battery.quickactions

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionLowPowerMode(button: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(button, host) {

    private val lowerPowerMode by lazy { LowPowerMode(context) }
    private val prefs by lazy { UserPreferences(context) }

    private fun update() {
        setState(lowerPowerMode.isEnabled())
    }

    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.ToolBattery)
        update()
    }

    override fun onResume() {
        super.onResume()
        update()
    }

    override fun onClick() {
        super.onClick()
        if (lowerPowerMode.isEnabled()) {
            prefs.power.userEnabledLowPower = false
            lowerPowerMode.disable()
        } else {
            prefs.power.userEnabledLowPower = true
            lowerPowerMode.enable()
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().navigate(Route.Battery)
        return true
    }
}
