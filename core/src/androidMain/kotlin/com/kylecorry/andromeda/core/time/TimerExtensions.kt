package com.kylecorry.andromeda.core.time

import com.kylecorry.luna.time.ITimer
import kotlin.time.Duration

/**
 * kotlin.time overloads for Luna timers, whose own overloads take java.time.Duration or milliseconds.
 * Durations are truncated to whole milliseconds, the timers' resolution.
 */

/** Runs the timer's action every [period], first after [initialDelay]. */
fun ITimer.interval(period: Duration, initialDelay: Duration = Duration.ZERO) {
    interval(period.inWholeMilliseconds, initialDelay.inWholeMilliseconds)
}

/** Runs the timer's action once after [delay]. */
fun ITimer.once(delay: Duration) {
    once(delay.inWholeMilliseconds)
}
