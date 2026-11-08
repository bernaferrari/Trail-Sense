@file:Suppress("MaxLineLength", "ReturnCount")

package com.kylecorry.sol.time

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object Time {
    fun hours(hours: Double): Duration = (hours * 3600000).toLong().milliseconds
    fun hours(duration: Duration): Double = duration.inWholeMilliseconds / 3600000.0
    fun duration(hours: Long = 0, minutes: Long = 0, seconds: Long = 0): Duration =
        hours.hours + minutes.minutes + seconds.seconds
    fun Instant.isInPast(): Boolean = this < Clock.System.now()
    fun Instant.isOlderThan(duration: Duration): Boolean = this < Clock.System.now() - duration
    fun Instant.hoursUntil(other: Instant): Float = secondsBetween(this, other) / 3600f
    fun LocalDate.daysUntil(other: LocalDate): Long = other.toEpochDays().toLong() - toEpochDays()
    fun Instant.plusHours(hours: Long): Instant = this + hours.hours
    fun hoursBetween(first: Instant, second: Instant): Float = secondsBetween(first, second) / 3600f
    private fun secondsBetween(first: Instant, second: Instant): Long = second.epochSeconds - first.epochSeconds -
        if (second.nanosecondsOfSecond < first.nanosecondsOfSecond) 1 else 0
    fun getClosestPastTime(time: Instant, times: List<Instant?>): Instant? = times.filterNotNull().filter { it < time }.maxOrNull()
    fun getClosestFutureTime(time: Instant, times: List<Instant?>): Instant? = times.filterNotNull().filter { it > time }.minOrNull()
    fun getClosestTime(time: Instant, times: List<Instant?>): Instant? =
        times.filterNotNull().minByOrNull { (it - time).absoluteValue }

    inline fun <T> getReadings(start: Instant, end: Instant, step: Duration, alwaysIncludeEnd: Boolean = true,
                               valueFn: (Instant) -> T): List<Reading<T>> {
        if (step <= Duration.ZERO) return emptyList()
        val readings = mutableListOf<Reading<T>>()
        var time = start
        var recordedEnd = false
        while (time <= end) {
            readings.add(Reading(valueFn(time), time))
            if (time == end) recordedEnd = true
            time += step
        }
        if (!recordedEnd && alwaysIncludeEnd && start <= end) readings.add(Reading(valueFn(end), end))
        return readings
    }
    inline fun <T> getReadings(date: LocalDate, zone: TimeZone, step: Duration,
                               alwaysIncludeEndOfDay: Boolean = true, valueFn: (Instant) -> T): List<Reading<T>> {
        val end = LocalDateTime(date.year, date.month, date.day, 23, 59, 59, 999999999).toInstant(zone)
        return getReadings(date.atStartOfDayIn(zone), end, step, alwaysIncludeEndOfDay, valueFn)
    }
    inline fun <T> getYearlyValues(year: Int, valueProvider: (LocalDate) -> T): List<Pair<LocalDate, T>> {
        val values = mutableListOf<Pair<LocalDate, T>>()
        var date = LocalDate(year, 1, 1)
        while (date.year == year) {
            values.add(date to valueProvider(date))
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return values
    }
    fun List<Reading<*>>.duration(): Duration {
        val start = minByOrNull { it.time } ?: return Duration.ZERO
        val end = maxByOrNull { it.time } ?: return Duration.ZERO
        return end.time - start.time
    }
    fun getDaylightSavings(time: Instant, zone: TimeZone): Duration =
        (zone.offsetAt(time).totalSeconds - standardOffsetSeconds(zone, time)).seconds
    fun isDaylightSavings(time: Instant, zone: TimeZone): Boolean = getDaylightSavings(time, zone) != Duration.ZERO
    fun getDaylightSavingsTransitions(zone: TimeZone, year: Int): List<Pair<Instant, Duration>> {
        val transitions = mutableListOf<Pair<Instant, Duration>>()
        var time = LocalDate(year, 1, 1).atStartOfDayIn(zone)
        repeat(100) {
            val next = nextTimeZoneTransition(zone, time) ?: return transitions
            if (next.toLocalDateTime(zone).year != year) return transitions
            val savings = getDaylightSavings(next, zone)
            if (transitions.lastOrNull()?.second != savings) transitions.add(next to savings)
            time = next
        }
        return transitions
    }
    fun Range<Instant>.middle(): Instant = start + (end - start) / 2
    fun Instant.roundNearestMinute(minutes: Int = 1, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant {
        val local = toLocalDateTime(timeZone)
        val nanosPerMinute = 60000000000L
        val interval = minutes * nanosPerMinute
        val nanos = local.minute * nanosPerMinute + local.second * 1000000000L + local.nanosecond
        val rounded = ((nanos + interval / 2) / interval) * interval
        return this + (rounded - nanos).nanoseconds
    }
    fun getSolarTimeOffset(longitude: Double): Duration = hours(longitude / 15)
    fun getLongitudeFromSolarTimeOffset(offset: Duration): Double = hours(offset) * 15
    fun getApproximateTimeZone(location: Coordinate): TimeZone {
        val hours = (location.longitude / 15).roundToInt()
        val sign = if (hours >= 0) "+" else "-"
        return TimeZone.of("UTC$sign${hours.absoluteValue.toString().padStart(2, '0')}:00")
    }
    fun getLocationFromTimeZone(zone: TimeZone, time: Instant = Clock.System.now()): Coordinate =
        TimeZoneLocations.getLocation(zone.id) ?: Coordinate(0.0, standardOffsetSeconds(zone, time) / 240.0)
}
