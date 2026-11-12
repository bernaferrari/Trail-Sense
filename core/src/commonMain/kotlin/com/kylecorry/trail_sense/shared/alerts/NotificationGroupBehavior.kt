package com.kylecorry.trail_sense.shared.alerts

/** Whether alerts stay in the system's notification group or are pulled out so they show on their own. */
enum class NotificationGroupBehavior {
    System,
    UngroupAll,
    UngroupHigh
}
