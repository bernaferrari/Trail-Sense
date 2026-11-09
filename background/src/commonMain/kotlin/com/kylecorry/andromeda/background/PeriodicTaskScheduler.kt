package com.kylecorry.andromeda.background

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

public interface PeriodicTaskScheduler : TaskScheduler {
    public fun interval(period: Duration, initialDelay: Duration = Duration.ZERO)
    public fun interval(period: Duration, start: Instant): Unit = interval(period, (start - Clock.System.now()).coerceAtLeast(Duration.ZERO))
}
