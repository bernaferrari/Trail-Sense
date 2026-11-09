package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.data.Identifiable
import kotlin.time.Duration
import kotlin.time.Instant

data class StepCountBucket(
    override val id: Long,
    val periodId: Long,
    val startTime: Instant,
    val endTime: Instant,
    val steps: Long,
    val activeTime: Duration = Duration.ZERO
) : Identifiable
