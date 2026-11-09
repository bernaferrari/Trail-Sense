package com.kylecorry.trail_sense.shared.andromeda_temp.background

import kotlin.time.Duration
import kotlin.time.Instant

interface IOneTimeTaskScheduler: ITaskScheduler {
    fun once(delay: Duration = Duration.ZERO)
    fun once(time: Instant)
    fun start()
}