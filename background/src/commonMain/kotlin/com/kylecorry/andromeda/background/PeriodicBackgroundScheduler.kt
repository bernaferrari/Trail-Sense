package com.kylecorry.andromeda.background

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Platform-managed recurring work, subject to operating-system scheduling and power policies. */
interface PeriodicBackgroundScheduler : IPeriodicTaskScheduler {
    /** Call when the platform invokes the task; some platforms use this to schedule the next run. */
    fun onTaskStarted()
}

internal fun requirePeriodicSchedule(period: Duration, initialDelay: Duration) {
    require(period.isFinite() && period >= 15.minutes) { "The interval must be at least 15 minutes" }
    require(initialDelay.isFinite() && initialDelay >= Duration.ZERO) { "The initial delay must be finite and nonnegative" }
}
