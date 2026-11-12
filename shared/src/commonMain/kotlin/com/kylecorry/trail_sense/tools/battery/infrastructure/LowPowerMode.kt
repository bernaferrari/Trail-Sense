package com.kylecorry.trail_sense.tools.battery.infrastructure

import com.kylecorry.trail_sense.main.getAppService

import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.tools.battery.BatteryBroadcasts

class LowPowerMode {

    private val prefs by lazy { getAppService<UserPreferences>() }

    fun enable() {
        prefs.isLowPowerModeOn = true
        ToolBroadcasts.broadcast(BatteryBroadcasts.POWER_SAVING_MODE_ENABLED)
    }

    fun disable() {
        prefs.isLowPowerModeOn = false
        ToolBroadcasts.broadcast(BatteryBroadcasts.POWER_SAVING_MODE_DISABLED)
    }

    fun isEnabled(): Boolean {
        return prefs.isLowPowerModeOn
    }
}
