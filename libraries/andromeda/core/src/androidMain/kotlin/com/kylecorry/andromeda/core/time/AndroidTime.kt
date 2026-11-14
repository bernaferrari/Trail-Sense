package com.kylecorry.andromeda.core.time

import android.os.SystemClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

public object AndroidTime {

    /**
     * Gets the time in milliseconds since boot (excluding time spent in deep sleep)
     * @return the uptime in milliseconds
     */
    public fun uptimeMillis(): Long {
        return SystemClock.uptimeMillis()
    }

    /**
     * Gets the time since boot (excluding time spent in deep sleep)
     * @return the uptime
     */
    public fun uptime(): Duration {
        return uptimeMillis().milliseconds
    }

    /**
     * Gets the time in milliseconds since boot (including time spent in deep sleep)
     * @return the time since boot
     */
    public fun millisSinceBoot(): Long {
        return SystemClock.elapsedRealtime()
    }

    /**
     * Gets the time since boot (including time spent in deep sleep)
     * @return the time since boot
     */
    public fun timeSinceBoot(): Duration {
        return millisSinceBoot().milliseconds
    }

    /**
     * Converts a time in milliseconds since boot to Unix time (milliseconds since epoch)
     * @param millisSinceBoot the time since boot
     * @return the Unix time in milliseconds
     */
    public fun millisSinceBootToTime(millisSinceBoot: Long): Long {
        val currentTimeSinceBoot = millisSinceBoot()
        val delta = currentTimeSinceBoot - millisSinceBoot
        return System.currentTimeMillis() - delta
    }

    /**
     * Converts a time in milliseconds since boot to an Instant
     * @param millisSinceBoot the time since boot
     * @return the Instant
     */
    public fun millisSinceBootToInstant(millisSinceBoot: Long): Instant {
        return Instant.fromEpochMilliseconds(millisSinceBootToTime(millisSinceBoot))
    }

}