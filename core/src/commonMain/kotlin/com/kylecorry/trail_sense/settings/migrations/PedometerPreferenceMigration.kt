package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.luna.concurrency.onIO

import com.kylecorry.trail_sense.shared.time.truncateTo

import kotlin.time.Clock

import kotlin.time.Duration.Companion.hours


import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.tools.pedometer.domain.ActiveTimeCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import kotlin.time.Duration
import kotlin.time.Instant

class PedometerPreferenceMigration(
    private val stepTracker: IStepTrackerService,
    private val prefs: Preferences,
    private val now: () -> Instant = { Clock.System.now() }
) {
    private val activeTimeCalculator = ActiveTimeCalculator()

    suspend fun migrate(): Unit = onIO {
        val steps = prefs.getLong(STEPS_KEY) ?: 0L
        val startTime = prefs.getInstant(LAST_RESET_KEY)
        if (steps > 0 && startTime != null) {
            var remainingSteps = steps
            createStepAdditions(startTime, now().coerceAtLeast(startTime), steps).forEach {
                stepTracker.addSteps(
                    it.steps,
                    it.time,
                    activeTimeCalculator.calculate(it.steps, it.elapsedTime)
                )
                remainingSteps -= it.steps
                prefs.putLong(STEPS_KEY, remainingSteps)
                prefs.putInstant(LAST_RESET_KEY, it.endTime)
            }
        }
        prefs.remove(STEPS_KEY)
        prefs.remove(LAST_RESET_KEY)
    }

    private fun createStepAdditions(
        startTime: Instant,
        endTime: Instant,
        steps: Long
    ): List<StepAddition> {
        val bucketRanges = getStepCountBucketRanges(startTime, endTime)
        val baseSteps = steps / bucketRanges.size
        val remainingSteps = steps % bucketRanges.size
        return bucketRanges.mapIndexed { index, range ->
            StepAddition(
                time = range.first,
                steps = baseSteps + if (index.toLong() < remainingSteps) 1 else 0,
                elapsedTime = (minOf(range.second, endTime) - range.first),
                endTime = minOf(range.second, endTime)
            )
        }
    }

    private fun getStepCountBucketRanges(
        startTime: Instant,
        endTime: Instant
    ): List<Pair<Instant, Instant>> {
        val ranges = mutableListOf<Pair<Instant, Instant>>()
        var bucketStart = startTime
        do {
            val nextHour = bucketStart.truncateTo(1.hours) + 1.hours
            ranges.add(bucketStart to nextHour)
            bucketStart = nextHour
        } while (bucketStart < endTime)
        return ranges
    }

    private data class StepAddition(
        val time: Instant,
        val steps: Long,
        val elapsedTime: Duration,
        val endTime: Instant
    )

    companion object {
        private const val STEPS_KEY = "cache_steps"
        private const val LAST_RESET_KEY = "last_odometer_reset"
    }
}
