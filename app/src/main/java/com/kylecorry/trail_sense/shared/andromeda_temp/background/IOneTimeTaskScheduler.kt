package com.kylecorry.trail_sense.shared.andromeda_temp.background

import java.time.Duration
import java.time.Instant

interface IOneTimeTaskScheduler: ITaskScheduler {
    fun once(delay: Duration = Duration.ZERO)
    fun once(time: Instant)
    fun start()
}