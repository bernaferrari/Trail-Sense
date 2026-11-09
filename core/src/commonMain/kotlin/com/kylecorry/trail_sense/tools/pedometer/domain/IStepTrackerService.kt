package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.time.Clock

import com.kylecorry.trail_sense.main.persistence.ICleanable
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

interface IStepTrackerService : ICleanable {
    suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod>

    suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod?

    suspend fun getHourlyStepCounts(
        date: LocalDate,
        zoneId: TimeZone = TimeZone.currentSystemDefault()
    ): List<HourlyStepCount>

    suspend fun startNewStepTrackingPeriod(endTime: Instant = Clock.System.now())

    suspend fun addSteps(
        steps: Long,
        time: Instant = Clock.System.now(),
        activeTime: Duration = Duration.ZERO
    )

    suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod)
}
