package com.kylecorry.trail_sense.tools.battery.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.tools.battery.BatteryToolRegistration

class LowPowerMode(val context: Context) {

    private val prefs by lazy { UserPreferences(context) }

    fun enable() {
        prefs.isLowPowerModeOn = true
        ToolBroadcasts.broadcast(BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_ENABLED)
    }

    fun disable() {
        prefs.isLowPowerModeOn = false
        ToolBroadcasts.broadcast(BatteryToolRegistration.BROADCAST_POWER_SAVING_MODE_DISABLED)
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}
