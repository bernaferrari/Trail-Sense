package com.kylecorry.andromeda.background

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

interface PeriodicTaskScheduler : TaskScheduler {
    fun interval(period: Duration, initialDelay: Duration = Duration.ZERO)
    fun interval(period: Duration, start: Instant) = interval(period, (start - Clock.System.now()).coerceAtLeast(Duration.ZERO))
}
