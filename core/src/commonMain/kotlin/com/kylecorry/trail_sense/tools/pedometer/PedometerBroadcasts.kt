package com.kylecorry.trail_sense.tools.pedometer

/** Tool broadcasts the pedometer sends; the values are stable ids other tools subscribe to. */
object PedometerBroadcasts {
    const val PEDOMETER_ENABLED = "pedometer-broadcast-pedometer-enabled"
    const val PEDOMETER_DISABLED = "pedometer-broadcast-pedometer-disabled"
    const val STEPS_CHANGED = "pedometer-broadcast-steps-changed"
    const val DISTANCE_CHANGED = "pedometer-broadcast-distance-changed"
    const val PARAM_STEPS = "pedometer-broadcast-param-steps"
}
