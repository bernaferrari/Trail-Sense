package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.time.Instant

data class HourlyStepCount(
    val startTime: Instant,
    val endTime: Instant,
    val steps: Long
)
