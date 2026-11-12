package com.kylecorry.trail_sense.shared.device

/**
 * Facts about the device and its locale that settings depend on, so preference code doesn't reach for a platform API.
 */
interface DeviceProfile {
    /** A usable compass, false when the hardware is missing or a debug setting hides it. */
    val hasCompass: Boolean
    val hasGyroscope: Boolean
    val hasBarometer: Boolean
    val hasHygrometer: Boolean

    /** Whether the system can supply colors from the wallpaper. */
    val hasDynamicColors: Boolean

    /** Whether the locale measures in metric units, used to pick unit defaults. */
    val isMetricPreferred: Boolean
    val uses24HourClock: Boolean
}
