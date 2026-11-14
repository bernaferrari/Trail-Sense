package com.kylecorry.sol.time

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.tests.zoned
import com.kylecorry.sol.time.Time.daysUntil
import com.kylecorry.sol.time.Time.getClosestFutureTime
import com.kylecorry.sol.time.Time.getClosestPastTime
import com.kylecorry.sol.time.Time.getClosestTime
import com.kylecorry.sol.time.Time.hoursUntil
import com.kylecorry.sol.time.Time.isInPast
import com.kylecorry.sol.time.Time.isOlderThan
import com.kylecorry.sol.time.Time.middle
import com.kylecorry.sol.time.Time.plusHours
import com.kylecorry.sol.time.Time.roundNearestMinute
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimeTest {

    private val newYork = TimeZone.of("America/New_York")

    @Test
    fun hours() {
        hours(100.0, "PT100H")
        hours(24.0, "PT24H")
        hours(1.5, "PT1H30M")
        hours(1.0, "PT1H")
        hours(0.5, "PT30M")
        hours(0.03125, "PT1M52S")
        hours(0.00048828125, "PT1S")
    }

    private fun hours(hours: Double, expectedIso: String) {
        val expected = Duration.parse(expectedIso)
        assertDurationEquals(expected, Time.hours(hours), 1.seconds)
        assertEquals(hours, Time.hours(Time.hours(hours)), 0.0001)
    }

    @Test
    fun instantIsInPast() {
        val now = Clock.System.now()
        assertTrue((now - 1.seconds).isInPast())
        assertFalse((now + 10.seconds).isInPast())
    }

    @Test
    fun instantIsOlderThan() {
        val time = Clock.System.now() - 100.seconds
        assertTrue(time.isOlderThan(10.seconds))
        assertFalse(time.isOlderThan(110.seconds))
    }

    @Test
    fun duration() {
        assertEquals(1.hours + 2.minutes + 3.seconds, Time.duration(1, 2, 3))
        assertEquals(Duration.ZERO, Time.duration())
    }

    @Test
    fun hoursUntil() {
        val start = Instant.fromEpochSeconds(1_600_000_000, 500_000_000)
        assertEquals(1f, start.hoursUntil(start + 3600.seconds))
        assertEquals(-1f, start.hoursUntil(start - 3600.seconds))
    }

    @Test
    fun daysBetweenLocalDates() {
        assertEquals(1L, LocalDate(2020, 1, 1).daysUntil(LocalDate(2020, 1, 2)))
        assertEquals(366L, LocalDate(2020, 1, 1).daysUntil(LocalDate(2021, 1, 1)))
        assertEquals(-1L, LocalDate(2020, 1, 2).daysUntil(LocalDate(2020, 1, 1)))
    }

    @Test
    fun instantPlusHours() {
        val time = Instant.fromEpochSeconds(1_600_000_000)
        assertEquals(time + 1.hours, time.plusHours(1))
    }

    @Test
    fun getDaylightSavingsTransitions() {
        val times = Time.getDaylightSavingsTransitions(newYork, 2022)
        assertEquals(
            listOf(
                Instant.parse("2022-03-13T07:00:00Z") to 1.hours,
                Instant.parse("2022-11-06T06:00:00Z") to Duration.ZERO,
            ),
            times
        )
    }

    @Test
    fun getDaylightSavingsTransitionsNotObserved() {
        assertEquals(emptyList(), Time.getDaylightSavingsTransitions(TimeZone.of("Asia/Shanghai"), 2022))
    }

    @Test
    fun isDaylightSavings() {
        assertTrue(Time.isDaylightSavings(zoned(2022, 3, 13, 2, zone = newYork).instant, newYork))
        assertFalse(Time.isDaylightSavings(zoned(2022, 11, 6, 2, zone = newYork).instant, newYork))
        assertFalse(Time.isDaylightSavings(Instant.parse("2022-07-01T00:00:00Z"), TimeZone.UTC))
    }

    @Test
    fun getDaylightSavings() {
        assertEquals(1.hours, Time.getDaylightSavings(zoned(2022, 3, 13, 2, zone = newYork).instant, newYork))
        assertEquals(Duration.ZERO, Time.getDaylightSavings(zoned(2022, 11, 6, 2, zone = newYork).instant, newYork))
    }

    @Test
    fun canGetClosestPastTime() {
        val now = utc(2020, 1, 10, 2)
        val times = listOf(utc(2020, 1, 10, 0), utc(2020, 1, 11, 0), utc(2020, 1, 10, 1), null)
        assertEquals(utc(2020, 1, 10, 1), getClosestPastTime(now, times))
    }

    @Test
    fun canGetClosestPastTimeSubSecond() {
        val now = Clock.System.now()
        val times = listOf(now - 100.seconds, now - 10.seconds, now + 100.seconds, null)
        assertEquals(now - 10.seconds, getClosestPastTime(now, times))
    }

    @Test
    fun canGetClosestTime() {
        val now = utc(2020, 1, 10, 2)
        val times = listOf(
            utc(2020, 1, 10, 0),
            utc(2020, 1, 11, 0),
            utc(2020, 1, 10, 1),
            utc(2020, 1, 10, 2, 30),
            null
        )
        assertEquals(utc(2020, 1, 10, 2, 30), getClosestTime(now, times))
    }

    @Test
    fun canGetClosestFutureTime() {
        val now = utc(2020, 1, 10, 2)
        val times = listOf(utc(2020, 1, 10, 0), utc(2020, 1, 11, 0), utc(2020, 1, 10, 1), null)
        assertEquals(utc(2020, 1, 11, 0), getClosestFutureTime(now, times))
    }

    @Test
    fun returnsNullIfNoFutureTimes() {
        val now = utc(2020, 1, 10, 2)
        assertNull(getClosestFutureTime(now, listOf(utc(2020, 1, 10, 0), utc(2020, 1, 10, 1), null)))
    }

    @Test
    fun returnsNullIfNoPastTimes() {
        val now = utc(2020, 1, 9, 2)
        assertNull(getClosestPastTime(now, listOf(utc(2020, 1, 10, 0), utc(2020, 1, 10, 1), null)))
    }

    @Test
    fun hoursBetween() {
        assertEquals(1f, Time.hoursBetween(utc(2020, 1, 1, 12), utc(2020, 1, 1, 13)))
        assertEquals(-1.5f, Time.hoursBetween(utc(2020, 1, 1, 13, 30), utc(2020, 1, 1, 12)))
    }

    @Test
    fun getReadingsDate() {
        val date = LocalDate(2020, 1, 1)
        val readings = Time.getReadings(date, newYork, 1.hours) { it.epochSeconds }
        assertEquals(25, readings.size)
        val start = date.atStartOfDayIn(newYork)
        val end = LocalDateTime(2020, 1, 1, 23, 59, 59, 999_999_999).toInstant(newYork)

        assertEquals(start.epochSeconds, readings.first().value)
        assertEquals(start, readings.first().time)
        assertEquals(end.epochSeconds, readings.last().value)
        assertEquals(end, readings.last().time)
    }

    @Test
    fun getReadingsDateWithoutEndOfDay() {
        val date = LocalDate(2020, 1, 1)
        val readings = Time.getReadings(date, newYork, 1.hours, false) { it.epochSeconds }
        assertEquals(24, readings.size)
        assertEquals(date.atStartOfDayIn(newYork), readings.first().time)
        assertEquals(LocalDateTime(2020, 1, 1, 23, 0).toInstant(newYork), readings.last().time)
    }

    @Test
    fun getReadingsDateAcrossDaylightSavings() {
        // The spring-forward day only has 23 hours
        val readings = Time.getReadings(LocalDate(2022, 3, 13), newYork, 1.hours, false) { it }
        assertEquals(23, readings.size)
    }

    @Test
    fun getReadingsIncludesEndWhenNotOnStep() {
        val start = utc(2020, 1, 1, 0)
        val end = utc(2020, 1, 1, 2, 30)
        val readings = Time.getReadings(start, end, 1.hours) { it.epochSeconds }
        val expected = listOf(start, start + 1.hours, start + 2.hours, end)
        assertEquals(expected, readings.map { it.time })
        assertEquals(expected.map { it.epochSeconds }, readings.map { it.value })
    }

    @Test
    fun getReadingsExcludesEndWhenNotOnStep() {
        val start = utc(2020, 1, 1, 0)
        val end = utc(2020, 1, 1, 2, 30)
        val readings = Time.getReadings(start, end, 1.hours, false) { it }
        assertEquals(listOf(start, start + 1.hours, start + 2.hours), readings.map { it.time })
    }

    @Test
    fun getReadingsDoesNotDuplicateEndWhenOnStep() {
        val start = utc(2020, 1, 1, 0)
        val end = utc(2020, 1, 1, 2)
        val readings = Time.getReadings(start, end, 1.hours) { it }
        assertEquals(listOf(start, start + 1.hours, end), readings.map { it.time })
    }

    @Test
    fun getReadingsStartEqualsEnd() {
        val time = utc(2020, 1, 1, 0)
        assertEquals(listOf(time), Time.getReadings(time, time, 1.hours) { it }.map { it.time })
    }

    @Test
    fun getReadingsWithoutPositiveStepIsEmpty() {
        val start = utc(2020, 1, 1, 0)
        val end = utc(2020, 1, 1, 23)
        assertEquals(0, Time.getReadings(start, end, Duration.ZERO, true) { it }.size)
        assertEquals(0, Time.getReadings(start, end, Duration.ZERO) { it }.size)
        assertEquals(0, Time.getReadings(start, end, (-1).hours) { it }.size)
    }

    @Test
    fun getReadingsSpan() {
        val start = utc(2020, 1, 1, 0)
        val end = utc(2020, 1, 1, 23)
        val readings = Time.getReadings(start, end, 1.hours) { it.epochSeconds }
        assertEquals(24, readings.size)
        assertEquals(start, readings.first().time)
        assertEquals(start.epochSeconds, readings.first().value)
        assertEquals(end, readings.last().time)
        assertEquals(end.epochSeconds, readings.last().value)
    }

    @Test
    fun getReadingsBackwards() {
        assertEquals(0, Time.getReadings(utc(2020, 1, 1, 0), utc(2019, 12, 30, 0), 1.hours) { it }.size)
    }

    @Test
    fun getYearlyValues() {
        val values = Time.getYearlyValues(2020) { it.dayOfYear.toLong() }
        assertEquals(366, values.size)
        assertEquals(LocalDate(2020, 1, 1) to 1L, values.first())
        assertEquals(LocalDate(2020, 12, 31) to 366L, values.last())
    }

    @Test
    fun durationOfReadings() {
        val readings = Time.getReadings(utc(2020, 1, 1, 0), utc(2020, 1, 1, 23), 1.hours) { it }
        with(Time) {
            assertEquals(23.hours, readings.duration())
            assertEquals(Duration.ZERO, emptyList<Reading<*>>().duration())
        }
    }

    @Test
    fun roundNearestMinute() {
        roundNearestMinute("2020-01-01T01:23:45-05:00", 1, "2020-01-01T01:24:00-05:00")
        roundNearestMinute("2020-01-01T01:23:24-05:00", 1, "2020-01-01T01:23:00-05:00")
        roundNearestMinute("2020-01-01T01:23:45-05:00", 5, "2020-01-01T01:25:00-05:00")
        roundNearestMinute("2020-01-01T01:23:45-05:00", 15, "2020-01-01T01:30:00-05:00")
        roundNearestMinute("2020-01-01T01:19:45-05:00", 15, "2020-01-01T01:15:00-05:00")
        roundNearestMinute("2020-01-01T01:07:59-05:00", 15, "2020-01-01T01:15:00-05:00")
        roundNearestMinute("2020-01-01T01:59:45-05:00", 15, "2020-01-01T02:00:00-05:00")
        roundNearestMinute("2020-01-01T23:59:45-05:00", 15, "2020-01-02T00:00:00-05:00")
        roundNearestMinute("2020-01-01T01:30:00-05:00", 15, "2020-01-01T01:30:00-05:00")
        // Rounds in the given zone, which matters for zones with non-hour offsets
        roundNearestMinute("2020-01-01T01:07:59+05:45", 15, "2020-01-01T01:15:00+05:45")
    }

    private fun roundNearestMinute(time: String, minutes: Int, expected: String) {
        val zonedTime = zoned(time)
        assertEquals(zoned(expected).instant, zonedTime.instant.roundNearestMinute(minutes, zonedTime.zone))
    }

    @Test
    fun roundNearestMinuteDefault() {
        val time = utc(2020, 1, 1, 1, 23) + 45.seconds + 500.nanoseconds
        assertEquals(utc(2020, 1, 1, 1, 24), time.roundNearestMinute(timeZone = TimeZone.UTC))
    }

    @Test
    fun middle() {
        assertEquals(utc(2020, 1, 1, 12), Range(utc(2020, 1, 1, 0), utc(2020, 1, 2, 0)).middle())
    }

    @Test
    fun getSolarTimeOffset() {
        val cases = listOf(
            0.0 to 0.0, 15.0 to 1.0, 30.0 to 2.0, 45.0 to 3.0, 60.0 to 4.0, 180.0 to 12.0,
            -15.0 to -1.0, -30.0 to -2.0, -45.0 to -3.0, -60.0 to -4.0, -180.0 to -12.0,
            20.0 to 1.3333, -20.0 to -1.3333
        )
        for ((longitude, offset) in cases) {
            assertDurationEquals(Time.hours(offset), Time.getSolarTimeOffset(longitude), 1.minutes)
            assertEquals(longitude, Time.getLongitudeFromSolarTimeOffset(Time.hours(offset)), 0.001)
        }
    }

    @Test
    fun getApproximateTimeZone() {
        val cases = listOf(
            0.0 to 0, 15.0 to 1, 30.0 to 2, 45.0 to 3, 60.0 to 4, 180.0 to 12,
            -15.0 to -1, -30.0 to -2, -45.0 to -3, -60.0 to -4, -180.0 to -12,
            20.0 to 1, -20.0 to -1
        )
        for ((longitude, hours) in cases) {
            val zone = Time.getApproximateTimeZone(Coordinate(0.0, longitude))
            assertEquals(UtcOffset(hours = hours), zone.offsetAt(Instant.fromEpochSeconds(0)), "longitude $longitude")
        }
    }

    @Test
    fun getLocationFromTimeZone() {
        val fixed = Time.getLocationFromTimeZone(TimeZone.of("UTC+03:00"), Instant.parse("2022-01-01T00:00:00Z"))
        assertEquals(0.0, fixed.latitude, 0.0001)
        assertEquals(45.0, fixed.longitude, 0.0001)

        val west = Time.getLocationFromTimeZone(TimeZone.of("UTC-05:00"), Instant.parse("2022-07-01T00:00:00Z"))
        assertEquals(-75.0, west.longitude, 0.0001)
    }

    private fun assertDurationEquals(expected: Duration, actual: Duration, delta: Duration) {
        assertTrue((expected - actual).absoluteValue <= delta, "Expected $expected but was $actual")
    }

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Instant =
        LocalDateTime(year, month, day, hour, minute).toInstant(TimeZone.UTC)
}
