package com.kylecorry.trail_sense.tools.paths

/** Tool broadcasts the paths tool sends; the values are stable ids other tools subscribe to. */
object PathsBroadcasts {
    const val BACKTRACK_ENABLED = "paths-broadcast-backtrack-enabled"
    const val BACKTRACK_DISABLED = "paths-broadcast-backtrack-disabled"
    const val BACKTRACK_STATE_CHANGED = "paths-broadcast-backtrack-state-changed"
    const val BACKTRACK_FREQUENCY_CHANGED = "paths-broadcast-backtrack-frequency-changed"
    const val PATHS_CHANGED = "paths-broadcast-backtrack-paths-changed"
    const val PARAM_BACKTRACK_FREQUENCY = "paths-broadcast-param-backtrack-frequency"
}
