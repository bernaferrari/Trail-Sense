package com.kylecorry.andromeda.background

import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Uses the provided scope for short intervals and platform-managed work for longer intervals.
 * The host is responsible for connecting the platform task to the same work and, on platforms
 * that need it, keeping the process alive for short intervals.
 */
class AdaptivePeriodicScheduler(
    scope: CoroutineScope,
    private val background: PeriodicBackgroundScheduler,
    task: suspend () -> Unit,
    private val backgroundThreshold: Duration = 15.minutes,
) : IPeriodicTaskScheduler {
    private val foreground = CoroutineTaskScheduler(scope, task)

    init { require(backgroundThreshold.isFinite() && backgroundThreshold > Duration.ZERO) }

    override fun interval(period: Duration, initialDelay: Duration) {
        require(period.isFinite() && period > Duration.ZERO)
        require(initialDelay.isFinite() && initialDelay >= Duration.ZERO)
        cancel()
        if (period < backgroundThreshold) foreground.interval(period, initialDelay)
        else background.interval(period, initialDelay)
    }

    override fun cancel() {
        foreground.cancel()
        background.cancel()
    }
}
