package com.kylecorry.trail_sense.shared.permissions

import com.kylecorry.trail_sense.shared.alerts.IAlerter

/**
 * Asks, once, to lift the platform's background battery restrictions for a background tool, explaining through
 * [IAlerter]. Platforms without such restrictions do nothing.
 */
fun interface BatteryRestrictionPrompt {
    fun request(alerter: IAlerter)
}
