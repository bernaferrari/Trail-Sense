package com.kylecorry.trail_sense.shared.andromeda_temp.background

import java.time.Duration
import java.time.Instant

interface IPeriodicTaskScheduler: ITaskScheduler {
    fun interval(period: Duration, initialDelay: Duration = Duration.ZERO)
    fun interval(period: Duration, start: Instant)
}