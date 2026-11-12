package com.kylecorry.trail_sense.settings.migrations

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.tools.pedometer.domain.HourlyStepCount
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackingPeriod
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class PedometerPreferenceMigrationTest {

    @Test
    fun migrateDistributesStepsAcrossHourBuckets() = runTest {
        val start = Instant.parse("2026-06-14T10:15:00Z")
        val end = Instant.parse("2026-06-14T12:30:00Z")
        val prefs = getPrefs(start, 10)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(4, start, 8.seconds),
                StepAddition(3, Instant.parse("2026-06-14T11:00:00Z"), 6.seconds),
                StepAddition(3, Instant.parse("2026-06-14T12:00:00Z"), 6.seconds)
            ),
            stepTracker.addedSteps
        )
        assertMigrated(prefs)
    }

    @Test
    fun migrateDistributesStepsAcrossEveryOverlappedHourBucket() = runTest {
        val start = Instant.parse("2026-06-14T01:50:00Z")
        val end = Instant.parse("2026-06-14T03:30:00Z")
        val prefs = getPrefs(start, 90)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(30, start, 60.seconds),
                StepAddition(30, Instant.parse("2026-06-14T02:00:00Z"), 60.seconds),
                StepAddition(30, Instant.parse("2026-06-14T03:00:00Z"), 60.seconds)
            ),
            stepTracker.addedSteps
        )
        assertMigrated(prefs)
    }

    @Test
    fun migrateAddsStepsWhenOpenPeriodExists() = runTest {
        val start = Instant.parse("2026-06-14T01:50:00Z")
        val end = Instant.parse("2026-06-14T03:30:00Z")
        val prefs = getPrefs(start, 90)
        val stepTracker = FakeStepTrackerService(
            openPeriod = StepTrackingPeriod(
                1,
                Instant.parse("2026-06-14T02:15:00Z"),
                null,
                emptyList()
            )
        )

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(30, start, 60.seconds),
                StepAddition(30, Instant.parse("2026-06-14T02:00:00Z"), 60.seconds),
                StepAddition(30, Instant.parse("2026-06-14T03:00:00Z"), 60.seconds)
            ),
            stepTracker.addedSteps
        )
    }

    @Test
    fun migrateCreatesOneAdditionWhenEndIsBeforeStart() = runTest {
        val start = Instant.parse("2026-06-14T10:15:00Z")
        val end = Instant.parse("2026-06-14T10:00:00Z")
        val prefs = getPrefs(start, 7)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(listOf(StepAddition(7, start, Duration.ZERO)), stepTracker.addedSteps)
        assertMigrated(prefs)
    }

    @Test
    fun migrateCapsActiveTimeToBucketElapsedTime() = runTest {
        val start = Instant.parse("2026-06-14T10:00:00Z")
        val end = Instant.parse("2026-06-14T10:01:00Z")
        val prefs = getPrefs(start, 100)
        val stepTracker = FakeStepTrackerService()

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(listOf(StepAddition(100, start, 1.minutes)), stepTracker.addedSteps)
    }

    @Test
    fun migrateCheckpointsCompletedAdditionsForRetry() = runTest {
        val start = Instant.parse("2026-06-14T10:15:00Z")
        val end = Instant.parse("2026-06-14T12:30:00Z")
        val prefs = getPrefs(start, 10)
        val stepTracker = FakeStepTrackerService(failOnAddition = 2)

        assertFailsWith<IllegalStateException> {
            PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()
        }

        // The first hour is saved, so a retry resumes after it
        assertEquals(listOf(StepAddition(4, start, 8.seconds)), stepTracker.addedSteps)
        assertEquals(6L, prefs.getLong(STEPS_KEY))
        assertEquals(Instant.parse("2026-06-14T11:00:00Z"), prefs.getInstant(LAST_RESET_KEY))

        PedometerPreferenceMigration(stepTracker, prefs) { end }.migrate()

        assertEquals(
            listOf(
                StepAddition(4, start, 8.seconds),
                StepAddition(3, Instant.parse("2026-06-14T11:00:00Z"), 6.seconds),
                StepAddition(3, Instant.parse("2026-06-14T12:00:00Z"), 6.seconds)
            ),
            stepTracker.addedSteps
        )
        assertMigrated(prefs)
    }

    private fun getPrefs(startTime: Instant, steps: Long): Preferences {
        val prefs = Preferences(MapSettings())
        prefs.putLong(STEPS_KEY, steps)
        prefs.putInstant(LAST_RESET_KEY, startTime)
        return prefs
    }

    private fun assertMigrated(prefs: Preferences) {
        assertFalse(prefs.contains(STEPS_KEY))
        assertFalse(prefs.contains(LAST_RESET_KEY))
    }

    private data class StepAddition(
        val steps: Long,
        val time: Instant,
        val activeTime: Duration
    )

    private class FakeStepTrackerService(
        private val openPeriod: StepTrackingPeriod? = null,
        private val failOnAddition: Int? = null
    ) : IStepTrackerService {
        val addedSteps = mutableListOf<StepAddition>()
        private var additionCount = 0

        override suspend fun getAllStepTrackingPeriods(): List<StepTrackingPeriod> = emptyList()

        override suspend fun getOpenStepTrackingPeriod(): StepTrackingPeriod? = openPeriod

        override suspend fun getHourlyStepCounts(date: LocalDate, zoneId: TimeZone): List<HourlyStepCount> =
            emptyList()

        override suspend fun startNewStepTrackingPeriod(endTime: Instant) {
        }

        override suspend fun addSteps(steps: Long, time: Instant, activeTime: Duration) {
            additionCount++
            if (additionCount == failOnAddition) {
                throw IllegalStateException("Failed to add steps")
            }
            addedSteps.add(StepAddition(steps, time, activeTime))
        }

        override suspend fun deleteStepTrackingPeriod(period: StepTrackingPeriod) {
        }

        override suspend fun clean() {
        }
    }

    private companion object {
        const val STEPS_KEY = "cache_steps"
        const val LAST_RESET_KEY = "last_odometer_reset"
    }
}
