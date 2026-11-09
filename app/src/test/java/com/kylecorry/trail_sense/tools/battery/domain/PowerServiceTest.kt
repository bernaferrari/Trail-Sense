package com.kylecorry.trail_sense.tools.battery.domain

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Instant

class PowerServiceTest {

    private val service = PowerService()
    private val start = Instant.fromEpochMilliseconds(1000000)

    @Test
    fun deepSleepIsTheElapsedTimeNotSpentAwake() {
        val readings = listOf(
            reading(Duration.ZERO, uptime = 30.minutes, elapsed = 30.minutes),
            reading(1.hours, uptime = 45.minutes, elapsed = 90.minutes)
        )

        val deepSleep = service.getDeepSleep(readings)!!
        assertEquals(75f, deepSleep.percent, 0.01f)
        assertEquals(60.minutes, deepSleep.duration)
    }

    @Test
    fun deepSleepIsAveragedOverAllIntervals() {
        val readings = listOf(
            reading(Duration.ZERO, uptime = Duration.ZERO, elapsed = Duration.ZERO),
            // Fully awake for an hour
            reading(1.hours, uptime = 1.hours, elapsed = 1.hours),
            // Fully asleep for an hour
            reading(2.hours, uptime = 1.hours, elapsed = 2.hours)
        )

        val deepSleep = service.getDeepSleep(readings)!!
        assertEquals(50f, deepSleep.percent, 0.01f)
        assertEquals(2.hours, deepSleep.duration)
    }

    @Test
    fun deepSleepIgnoresIntervalsWhereTheClocksReset() {
        val readings = listOf(
            reading(Duration.ZERO, uptime = 10.hours, elapsed = 20.hours),
            // Reboot - the clocks restart, so this interval can't be used
            reading(1.hours, uptime = 15.minutes, elapsed = 15.minutes),
            reading(2.hours, uptime = 30.minutes, elapsed = 75.minutes)
        )

        // Only the post-reboot interval counts, so the duration covers that hour alone
        val deepSleep = service.getDeepSleep(readings)!!
        assertEquals(75f, deepSleep.percent, 0.01f)
        assertEquals(60.minutes, deepSleep.duration)
    }

    @Test
    fun deepSleepIsNullWithoutAComparableInterval() {
        assertNull(service.getDeepSleep(emptyList()))
        assertNull(
            service.getDeepSleep(
                listOf(reading(Duration.ZERO, Duration.ZERO, Duration.ZERO))
            )
        )
        // Readings recorded before the clocks were tracked
        assertNull(
            service.getDeepSleep(
                listOf(
                    BatteryReading(start, 100f, 0f, false),
                    BatteryReading(start.plus(1.hours), 90f, 0f, false)
                )
            )
        )
    }

    private fun reading(
        offset: Duration,
        uptime: Duration,
        elapsed: Duration
    ): BatteryReading {
        return BatteryReading(start.plus(offset), 100f, 0f, false, uptime, elapsed)
    }
}
