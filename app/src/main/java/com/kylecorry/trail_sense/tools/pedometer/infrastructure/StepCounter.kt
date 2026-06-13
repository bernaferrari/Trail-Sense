package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepCountBucket
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import java.time.Duration
import java.time.Instant

internal class StepCounter(private val preferences: IPreferences) : IStepTrackerService {

    val startTime: Instant?
        get() = preferences.getInstant(LAST_RESET_KEY)


    val steps: Long
        get() = preferences.getLong(STEPS_KEY) ?: 0L

    fun addSteps(steps: Long) {
        synchronized(this) {
            val lastSteps = this.steps
            preferences.putLong(STEPS_KEY, steps + lastSteps)
            if (startTime == null) {
                preferences.putInstant(LAST_RESET_KEY, Instant.now())
            }
        }
    }

    override suspend fun addSteps(steps: Long, time: Instant) {
        addSteps(steps)
    }

    fun reset() {
        synchronized(this) {
            preferences.remove(STEPS_KEY)
            preferences.putInstant(LAST_RESET_KEY, Instant.now())
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
                    endTime = Instant.now().plus(Duration.ofHours(1)),
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
