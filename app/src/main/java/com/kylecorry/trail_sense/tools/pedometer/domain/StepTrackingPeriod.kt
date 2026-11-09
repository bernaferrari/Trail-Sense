package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.time.Clock

import com.kylecorry.trail_sense.shared.data.Identifiable
import kotlin.time.Duration
import kotlin.time.Instant

data class StepTrackingPeriod(
    override val id: Long,
    val startTime: Instant,
    val endTime: Instant?,
    val stepCountBuckets: List<StepCountBucket>
) : Identifiable {
    val isOpen = endTime == null
    val steps: Long
        get() = stepCountBuckets.sumOf { it.steps }

    val elapsedTime: Duration
        get() = (endTime ?: Clock.System.now()) - startTime

    val activeTime: Duration
        get() {
            val currentTime = Clock.System.now()
            val totalActiveTime = stepCountBuckets.fold(Duration.ZERO) { total, bucket ->
                val bucketActiveTime = if (bucket.steps > 0 && bucket.activeTime == Duration.ZERO) {
                    (minOf(bucket.endTime, currentTime) - bucket.startTime)
                } else {
                    bucket.activeTime
                }
                total.plus(bucketActiveTime)
            }
            return minOf(totalActiveTime, elapsedTime)
        }

    fun getDuration(mode: AveragePaceTimeMode): Duration {
        return when (mode) {
            AveragePaceTimeMode.Active -> activeTime
            AveragePaceTimeMode.Elapsed -> elapsedTime
        }
    }
}
