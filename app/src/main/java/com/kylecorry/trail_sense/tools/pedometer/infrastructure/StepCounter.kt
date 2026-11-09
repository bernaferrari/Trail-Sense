package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

internal class StepCounter(private val preferences: Preferences) : IStepTrackerService {

    val startTime: Instant?
        get() = preferences.getInstant(LAST_RESET_KEY)


    val steps: Long
        get() = preferences.getLong(STEPS_KEY) ?: 0L

    fun addSteps(steps: Long) {
        synchronized(this) {
            val lastSteps = this.steps
            preferences.putLong(STEPS_KEY, steps + lastSteps)
            if (startTime == null) {
                preferences.putInstant(LAST_RESET_KEY, kotlin.time.Clock.System.now())
            }
        }
    }

    override suspend fun addSteps(steps: Long, time: Instant, activeTime: Duration) {
        addSteps(steps)
    }

    override suspend fun getHourlyStepCounts(
        date: LocalDate,
        zoneId: TimeZone
    ): List<HourlyStepCount> = emptyList()

    override suspend fun clean() = Unit

    fun reset() {
        synchronized(this) {
            preferences.remove(STEPS_KEY)
            preferences.putInstant(LAST_RESET_KEY, kotlin.time.Clock.System.now())
        }
    }

    override suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> {
        return listOfNotNull(getOpenStepTrackingPeriod())
    }

    override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
        val start = startTime ?: return null
        return StepTrackingPeriod(
            id = 0,
            startTime = start,
            endTime = null,
            stepCountBuckets = listOf(
                StepCountBucket(
                    id = 0,
                    periodId = 0,
                    startTime = start,
                    endTime = Clock.System.now().plus(1.hours),
                    steps = steps
                )
            )
        )
    }

    override suspend fun startNewStepTrackingPeriod(endTime: Instant) {
        reset()
    }

    override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) {
        reset()
    }

    companion object {
        const val STEPS_KEY = "cache_steps"
        const val LAST_RESET_KEY = "last_odometer_reset"
    }

}
