package com.kylecorry.andromeda.background

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

public interface OneTimeTaskScheduler : TaskScheduler {
    public fun once(delay: Duration = Duration.ZERO)
    public fun once(time: Instant): Unit = once((time - Clock.System.now()).coerceAtLeast(Duration.ZERO))
    public fun start(): Unit = once()
}
