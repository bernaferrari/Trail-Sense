package com.kylecorry.trail_sense.tools.astronomy

/** Tool broadcasts and services of the astronomy tool; the values are stable ids other tools subscribe to. */
object AstronomyBroadcasts {
    const val SUNSET_ALERTS_ENABLED = "astronomy-broadcast-sunset-alerts-enabled"
    const val SUNSET_ALERTS_DISABLED = "astronomy-broadcast-sunset-alerts-disabled"
    const val SUNSET_ALERTS_STATE_CHANGED = "astronomy-broadcast-sunset-alerts-state-changed"

    const val SUNRISE_ALERTS_ENABLED = "astronomy-broadcast-sunrise-alerts-enabled"
    const val SUNRISE_ALERTS_DISABLED = "astronomy-broadcast-sunrise-alerts-disabled"
    const val SUNRISE_ALERTS_STATE_CHANGED = "astronomy-broadcast-sunrise-alerts-state-changed"

    const val SERVICE_SUNSET_ALERTS = "astronomy-service-sunset-alerts"
    const val SERVICE_SUNRISE_ALERTS = "astronomy-service-sunrise-alerts"
    const val SERVICE_ASTRONOMY_ALERTS = "astronomy-service-astronomy-alerts"
}
