package com.kylecorry.trail_sense.tools.pedometer.domain

import com.kylecorry.trail_sense.shared.time.truncateTo

import com.kylecorry.trail_sense.shared.time.atStartOfDay

import com.kylecorry.trail_sense.shared.time.plusDays

import kotlin.time.Duration.Companion.hours

import com.kylecorry.andromeda.core.time.ZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.IEventEmitter
import com.kylecorry.trail_sense.tools.pedometer.PedometerBroadcasts
import com.kylecorry.trail_sense.tools.pedometer.domain.abstractions.IStepTrackerRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

class StepTrackerService(
    private val repository: IStepTrackerRepository,
    private val eventBus: IEventEmitter,
    private val preferences: IPedometerPreferences,
    private val timeProvider: ZonedDateTimeProvider = SystemZonedDateTimeProvider()
) : IStepTrackerService {

    private val stepMutex = Mutex()

    override suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> {
        return repository.getStepTrackingPeriods()
    }

    override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? {
        return repository.getOpenStepTrackingPeriod()
    }

    override suspend fun getHourlyStepCounts(
        date: LocalDate,
        zoneId: TimeZone
    ): List<HourlyStepCount> {
        val startTime = date.atStartOfDay(zoneId)
        val endTime = date.plusDays(1).atStartOfDay(zoneId)
        val buckets = repository.getStepCountBuckets(startTime, endTime)

        val hourlyStepCounts = mutableListOf<HourlyStepCount>()
        var hourStart = startTime
        while ((hourStart < endTime)) {
            val hourEnd = hourStart.plus(1.hours)
            val steps = buckets.sumOf { bucket ->
                getProportionalSteps(bucket, hourStart, hourEnd).toDouble()
            }.roundToLong()
            hourlyStepCounts.add(HourlyStepCount(hourStart, hourEnd, steps))
            hourStart = hourEnd
        }
        return hourlyStepCounts
    }

    private fun getProportionalSteps(
        bucket: StepCountBucket,
        startTime: Instant,
        endTime: Instant
    ): Float {
        val bucketDuration = (bucket.endTime - bucket.startTime).inWholeMilliseconds
        val overlapStart = max(bucket.startTime.toEpochMilliseconds(), startTime.toEpochMilliseconds())
        val overlapEnd = min(bucket.endTime.toEpochMilliseconds(), endTime.toEpochMilliseconds())
        val overlapDuration = overlapEnd - overlapStart
        return if (bucketDuration > 0 && overlapDuration > 0) {
            bucket.steps * overlapDuration / bucketDuration.toFloat()
        } else {
            0f
        }
    }

    override suspend fun startNewStepTrackingPeriod(endTime: Instant) = stepMutex.withLock {
        closeStepTrackingPeriodWithoutLock(endTime)
        val openPeriod = getOrCreateOpenStepTrackingPeriodWithoutLock(endTime)
        emitStepsChanged(openPeriod.steps)
    }

    private suspend fun closeStepTrackingPeriodWithoutLock(endTime: Instant) {
        val openPeriod = repository.getOpenStepTrackingPeriod() ?: return
        if (openPeriod.steps == 0L) {
            repository.deleteBucketsInPeriod(openPeriod.id)
            repository.deleteStepTrackingPeriod(openPeriod)
        } else {
            repository.upsertStepTrackingPeriod(openPeriod.copy(endTime = endTime))
        }
    }

    override suspend fun addSteps(
        steps: Long,
        time: Instant,
        activeTime: Duration
    ) = stepMutex.withLock {
        val existingOpenPeriod = getOrCreateOpenStepTrackingPeriodWithoutLock(time)
        val openPeriod = if ((time < existingOpenPeriod.startTime)) {
            existingOpenPeriod.copy(startTime = time).also {
                repository.upsertStepTrackingPeriod(it)
            }
        } else {
            existingOpenPeriod
        }
        val containedBucket = openPeriod.stepCountBuckets.firstOrNull {
            (time == it.startTime || (time > it.startTime)) && (time < it.endTime)
        }
        val bucketToAdd = if (containedBucket != null) {
            containedBucket.copy(
                steps = containedBucket.steps + steps,
                activeTime = containedBucket.activeTime.plus(activeTime)
            )
        } else {
            // Buckets always start on the hour and are 1 hour long
            val startTime = time.truncateTo(1.hours)
            val endTime = startTime + DEFAULT_BUCKET_DURATION
            StepCountBucket(
                id = 0,
                periodId = openPeriod.id,
                startTime = startTime,
                endTime = endTime,
                steps = steps,
                activeTime = activeTime
            )
        }
        repository.upsertStepCountBucket(bucketToAdd)
        emitStepsChanged(openPeriod.steps + steps)
    }

    private suspend fun getOrCreateOpenStepTrackingPeriodWithoutLock(startTime: Instant): StepTrackingPeriod {
        val openPeriod = repository.getOpenStepTrackingPeriod()
        if (openPeriod != null) {
            return openPeriod
        }

        val newPeriod = StepTrackingPeriod(
            id = 0,
            startTime = startTime,
            endTime = null,
            stepCountBuckets = emptyList()
        )
        val newId = repository.upsertStepTrackingPeriod(newPeriod)
        return newPeriod.copy(id = newId)
    }

    override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) = stepMutex.withLock {
        repository.deleteBucketsInPeriod(period.id)
        repository.deleteStepTrackingPeriod(period)
        emitStepsChanged(repository.getOpenStepTrackingPeriod()?.steps ?: 0)
    }

    override suspend fun clean() = stepMutex.withLock {
        val cutoff = timeProvider.getTime().instant - preferences.stepHistory
        val deletedBuckets = repository.deleteBucketsOlderThan(cutoff)
        val deletedPeriods = repository.deleteEmptyClosedPeriods()
        val updatedPeriods = repository.setMinimumPeriodStartTime(cutoff)
        if (deletedBuckets || deletedPeriods || updatedPeriods) {
            emitStepsChanged(repository.getOpenStepTrackingPeriod()?.steps ?: 0)
        }
    }

    private fun emitStepsChanged(steps: Long) {
        eventBus.broadcast(
            PedometerBroadcasts.STEPS_CHANGED,
            EventData().apply {
                putLong(PedometerBroadcasts.PARAM_STEPS, steps)
            }
        )
    }

    companion object {
        private val DEFAULT_BUCKET_DURATION = 1.hours
    }
}
