package com.kylecorry.sol.time

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
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

public object Time {

    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val SECONDS_PER_HOUR = 3600f
    private const val NANOS_PER_SECOND = 1_000_000_000L
    private const val NANOS_PER_MINUTE = 60 * NANOS_PER_SECOND
    private const val DEGREES_PER_HOUR = 15
    private const val MAX_TRANSITIONS_PER_YEAR = 100
    private const val SECONDS_PER_DEGREE_OF_LONGITUDE = 240.0

    /**
     * Converts a fractional number of hours to a duration (millisecond precision).
     */
    public fun hours(hours: Double): Duration = (hours * MILLIS_PER_HOUR).toLong().milliseconds

    /**
     * Converts a duration to a fractional number of hours (millisecond precision).
     */
    public fun hours(duration: Duration): Double = duration.inWholeMilliseconds / MILLIS_PER_HOUR

    public fun duration(hours: Long = 0, minutes: Long = 0, seconds: Long = 0): Duration {
        return hours.hours + minutes.minutes + seconds.seconds
    }

    public fun Instant.isInPast(): Boolean = this < Clock.System.now()

    public fun Instant.isOlderThan(duration: Duration): Boolean = this < Clock.System.now() - duration

    /**
     * The number of hours until [other], truncated to whole seconds.
     */
    public fun Instant.hoursUntil(other: Instant): Float = hoursBetween(this, other)

    public fun LocalDate.daysUntil(other: LocalDate): Long = other.toEpochDays() - toEpochDays()

    public fun Instant.plusHours(hours: Long): Instant = this + hours.hours

    /**
     * The number of hours from [first] to [second], truncated to whole seconds.
     */
    public fun hoursBetween(first: Instant, second: Instant): Float {
        return wholeSecondsBetween(first, second) / SECONDS_PER_HOUR
    }

    /**
     * The last instant of the day in [timeZone], one nanosecond before the next day starts.
     */
    public fun LocalDate.atEndOfDayIn(timeZone: TimeZone): Instant {
        return LocalDateTime(year, month, day, 23, 59, 59, 999_999_999).toInstant(timeZone)
    }

    public fun getClosestPastTime(time: Instant, times: List<Instant?>): Instant? {
        return times.filterNotNull().filter { it < time }.maxOrNull()
    }

    public fun getClosestFutureTime(time: Instant, times: List<Instant?>): Instant? {
        return times.filterNotNull().filter { it > time }.minOrNull()
    }

    public fun getClosestTime(time: Instant, times: List<Instant?>): Instant? {
        return times.filterNotNull().minByOrNull { (it - time).absoluteValue }
    }

    /**
     * Samples [valueFn] from [start] to [end] (inclusive) every [step].
     * @param alwaysIncludeEnd if true, [end] is sampled even when it does not fall on a step
     * @return the readings, or an empty list if the step is not positive
     */
    public inline fun <T> getReadings(
        start: Instant,
        end: Instant,
        step: Duration,
        alwaysIncludeEnd: Boolean = true,
        valueFn: (time: Instant) -> T
    ): List<Reading<T>> {
        if (step <= Duration.ZERO) {
            return emptyList()
        }

        val readings = mutableListOf<Reading<T>>()
        var time = start
        var hasRecordedEnd = false
        while (time <= end) {
            readings.add(Reading(valueFn(time), time))
            if (time == end) {
                hasRecordedEnd = true
            }
            time += step
        }

        if (!hasRecordedEnd && alwaysIncludeEnd && start <= end) {
            readings.add(Reading(valueFn(end), end))
        }

        return readings
    }

    /**
     * Samples [valueFn] every [step] over the given [date] in [zone].
     */
    public inline fun <T> getReadings(
        date: LocalDate,
        zone: TimeZone,
        step: Duration,
        alwaysIncludeEndOfDay: Boolean = true,
        valueFn: (time: Instant) -> T
    ): List<Reading<T>> {
        return getReadings(date.atStartOfDayIn(zone), date.atEndOfDayIn(zone), step, alwaysIncludeEndOfDay, valueFn)
    }

    public inline fun <T> getYearlyValues(year: Int, valueProvider: (date: LocalDate) -> T): List<Pair<LocalDate, T>> {
        val values = mutableListOf<Pair<LocalDate, T>>()
        var date = LocalDate(year, 1, 1)
        while (date.year == year) {
            values.add(date to valueProvider(date))
            date = date.plus(1, DateTimeUnit.DAY)
        }
        return values
    }

    /**
     * The time between the earliest and latest reading.
     */
    public fun List<Reading<*>>.duration(): Duration {
        val start = minByOrNull { it.time } ?: return Duration.ZERO
        val end = maxByOrNull { it.time } ?: return Duration.ZERO
        return end.time - start.time
    }

    /**
     * The amount of daylight savings in effect in [zone] at [time] (zero if none).
     */
    public fun getDaylightSavings(time: Instant, zone: TimeZone): Duration {
        return (zone.offsetAt(time).totalSeconds - standardOffsetSeconds(zone, time)).seconds
    }

    public fun isDaylightSavings(time: Instant, zone: TimeZone): Boolean {
        return getDaylightSavings(time, zone) != Duration.ZERO
    }

    /**
     * Gets the instants in [year] when the daylight savings amount of [zone] changes, with the new amount.
     */
    public fun getDaylightSavingsTransitions(zone: TimeZone, year: Int): List<Pair<Instant, Duration>> {
        val transitions = mutableListOf<Pair<Instant, Duration>>()
        var time = LocalDate(year, 1, 1).atStartOfDayIn(zone)
        repeat(MAX_TRANSITIONS_PER_YEAR) {
            val next = nextTimeZoneTransition(zone, time)
            if (next == null || next.toLocalDateTime(zone).year != year) {
                return transitions
            }
            val savings = getDaylightSavings(next, zone)
            if (transitions.lastOrNull()?.second != savings) {
                transitions.add(next to savings)
            }
            time = next
        }
        return transitions
    }

    public fun Range<Instant>.middle(): Instant = start + (end - start) / 2

    /**
     * Rounds to the nearest multiple of [minutes] past the hour in [timeZone].
     */
    public fun Instant.roundNearestMinute(
        minutes: Int = 1,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Instant {
        val local = toLocalDateTime(timeZone)
        val interval = minutes * NANOS_PER_MINUTE
        val nanosIntoHour = local.minute * NANOS_PER_MINUTE + local.second * NANOS_PER_SECOND + local.nanosecond
        val rounded = ((nanosIntoHour + interval / 2) / interval) * interval
        return this + (rounded - nanosIntoHour).nanoseconds
    }

    /**
     * The offset of local solar time from UTC at the given longitude.
     */
    public fun getSolarTimeOffset(longitude: Double): Duration = hours(longitude / DEGREES_PER_HOUR)

    public fun getLongitudeFromSolarTimeOffset(offset: Duration): Double = hours(offset) * DEGREES_PER_HOUR

    /**
     * Gets a fixed offset time zone (whole hours) based on the solar time at the location.
     */
    public fun getApproximateTimeZone(location: Coordinate): TimeZone {
        val hours = (location.longitude / DEGREES_PER_HOUR).roundToInt()
        val sign = if (hours >= 0) "+" else "-"
        val paddedHours = hours.absoluteValue.toString().padStart(2, '0')
        return TimeZone.of("UTC$sign$paddedHours:00")
    }

    /**
     * Gets a representative location for the time zone. Known zones map to their principal city,
     * others to the equator at the longitude of their standard offset at [time].
     */
    public fun getLocationFromTimeZone(zone: TimeZone, time: Instant = Clock.System.now()): Coordinate {
        return TimeZoneLocations.getLocation(zone.id)
            ?: Coordinate(0.0, standardOffsetSeconds(zone, time) / SECONDS_PER_DEGREE_OF_LONGITUDE)
    }

    private fun wholeSecondsBetween(first: Instant, second: Instant): Long {
        val borrow = if (second.nanosecondsOfSecond < first.nanosecondsOfSecond) 1 else 0
        return second.epochSeconds - first.epochSeconds - borrow
    }
}
