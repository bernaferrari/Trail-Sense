package com.kylecorry.andromeda.background

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

interface IOneTimeTaskScheduler : ITaskScheduler {
    fun once(delay: Duration = Duration.ZERO)
    fun once(time: Instant) = once((time - Clock.System.now()).coerceAtLeast(Duration.ZERO))
    fun start() = once()
}
