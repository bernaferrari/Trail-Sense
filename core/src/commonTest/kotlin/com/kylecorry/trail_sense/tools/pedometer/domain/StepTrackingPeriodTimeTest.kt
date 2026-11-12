package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Upstream's elapsed and active time tests, dropped by the fork's merge. */
class StepTrackingPeriodTimeTest {

    @Test
    fun elapsedTimeIsTimeBetweenStartAndEnd() {
        val period = period(
            startTime = Instant.parse("2026-01-01T10:00:00Z"),
            endTime = Instant.parse("2026-01-01T11:30:00Z")
        )

        assertEquals(90.minutes, period.elapsedTime)
    }

    @Test
    fun activeTimeIsTheSumOfBucketActiveTimes() {
        val period = period(
            endTime = Instant.parse("2026-01-01T01:00:00Z"),
            stepCountBuckets = listOf(
                bucket(id = 1, steps = 100, activeTime = 10.minutes),
                bucket(id = 2, steps = 200, activeTime = 15.minutes)
            )
        )

        assertEquals(25.minutes, period.activeTime)
    }

    @Test
    fun activeTimeUsesBucketDurationWhenBucketHasStepsAndNoActiveTime() {
        val period = period(
            endTime = Instant.parse("2026-01-01T01:00:00Z"),
            stepCountBuckets = listOf(
                bucket(
                    steps = 100,
                    startTime = Instant.parse("2026-01-01T00:10:00Z"),
                    endTime = Instant.parse("2026-01-01T00:40:00Z")
                )
            )
        )

        assertEquals(30.minutes, period.activeTime)
    }

    @Test
    fun activeTimeDoesNotUseBucketDurationWhenBucketHasNoSteps() {
        val period = period(
            endTime = Instant.parse("2026-01-01T01:00:00Z"),
            stepCountBuckets = listOf(bucket(steps = 0))
        )

        assertEquals(Duration.ZERO, period.activeTime)
    }

    @Test
    fun activeTimeCapsFallbackBucketEndTimeToCurrentTime() {
        val currentTime = Clock.System.now()
        val period = period(
            startTime = currentTime - 1.hours,
            endTime = currentTime + 1.hours,
            stepCountBuckets = listOf(
                bucket(
                    steps = 100,
                    startTime = currentTime - 30.minutes,
                    endTime = currentTime + 30.minutes
                )
            )
        )

        assertTrue(period.activeTime >= 30.minutes)
        assertTrue(period.activeTime < 31.minutes)
    }

    @Test
    fun activeTimeIsCappedToElapsedTime() {
        val period = period(
            startTime = Instant.parse("2026-01-01T00:15:00Z"),
            endTime = Instant.parse("2026-01-01T00:45:00Z"),
            stepCountBuckets = listOf(
                bucket(
                    steps = 100,
                    startTime = Instant.parse("2026-01-01T00:00:00Z"),
                    endTime = Instant.parse("2026-01-01T01:00:00Z")
                )
            )
        )

        assertEquals(30.minutes, period.activeTime)
    }

    private fun period(
        startTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        endTime: Instant? = null,
        stepCountBuckets: List<StepCountBucket> = emptyList()
    ): StepTrackingPeriod {
        return StepTrackingPeriod(1, startTime, endTime, stepCountBuckets)
    }

    private fun bucket(
        id: Long = 1,
        startTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
        endTime: Instant = Instant.parse("2026-01-01T00:01:00Z"),
        steps: Long = 0,
        activeTime: Duration = Duration.ZERO
    ): StepCountBucket {
        return StepCountBucket(id, 1, startTime, endTime, steps, activeTime)
    }
}
