package com.kylecorry.trail_sense.shared.notifications

/** One of the app's notification channels, as the platform's notification settings see it. */
interface NotificationChannelControl {
    /** Whether the user turned the channel off. */
    fun isBlocked(): Boolean

    /** Opens the platform's settings for the channel. */
    fun openSettings()
}
