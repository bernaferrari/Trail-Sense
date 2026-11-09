package com.kylecorry.trail_sense.shared.time

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.offsetAt
import kotlinx.datetime.UtcOffset

/*
 * Small conveniences on top of kotlin.time and kotlinx-datetime. Anything zone dependent defaults
 * to the device time zone, which is what the app shows to the user.
 */

val systemTimeZone: TimeZone
    get() = TimeZone.currentSystemDefault()

fun today(zone: TimeZone = systemTimeZone, clock: Clock = Clock.System): LocalDate = clock.todayIn(zone)

fun nowLocal(zone: TimeZone = systemTimeZone, clock: Clock = Clock.System): LocalDateTime =
    clock.now().toLocalDateTime(zone)

fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(systemTimeZone)

fun Instant.toLocalDate(zone: TimeZone = systemTimeZone): LocalDate = toLocalDateTime(zone).date

fun Instant.toLocalTime(zone: TimeZone = systemTimeZone): LocalTime = toLocalDateTime(zone).time

fun LocalDateTime.toInstant(): Instant = toInstant(systemTimeZone)

fun LocalDate.atStartOfDay(zone: TimeZone = systemTimeZone): Instant = atStartOfDayIn(zone)

/**
 * The instant of 23:59:59.999999999 on this date. Like `ZonedDateTime.of(date, LocalTime.MAX, zone)`,
 * an overlap resolves to the earlier offset and a gap shifts forward by the gap length.
 */
fun LocalDate.atEndOfDay(zone: TimeZone = systemTimeZone): Instant =
    atEndOfDayLocal().toInstant(zone)

fun LocalDate.atEndOfDayLocal(): LocalDateTime = atTime(LocalTime(23, 59, 59, 999_999_999))

fun Instant.atStartOfDay(zone: TimeZone = systemTimeZone): Instant = toLocalDate(zone).atStartOfDay(zone)

fun Instant.atEndOfDay(zone: TimeZone = systemTimeZone): Instant = toLocalDate(zone).atEndOfDay(zone)

fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.plusDays(days: Long): LocalDate = plus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Int): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.minusDays(days: Long): LocalDate = minus(days, DateTimeUnit.DAY)
fun LocalDate.plusWeeks(weeks: Int): LocalDate = plus(weeks, DateTimeUnit.WEEK)
fun LocalDate.minusWeeks(weeks: Int): LocalDate = minus(weeks, DateTimeUnit.WEEK)
fun LocalDate.plusMonths(months: Int): LocalDate = plus(months, DateTimeUnit.MONTH)
fun LocalDate.minusMonths(months: Int): LocalDate = minus(months, DateTimeUnit.MONTH)
fun LocalDate.plusYears(years: Int): LocalDate = plus(years, DateTimeUnit.YEAR)
fun LocalDate.minusYears(years: Int): LocalDate = minus(years, DateTimeUnit.YEAR)
fun LocalDate.withDayOfMonth(day: Int): LocalDate = LocalDate(year, month, day)
fun LocalDate.withYear(year: Int): LocalDate = LocalDate(year, month, day)
fun LocalDate.withDayOfYear(dayOfYear: Int): LocalDate = LocalDate(year, 1, 1).plusDays(dayOfYear - 1)
val LocalDate.lengthOfMonth: Int
    get() = LocalDate(year, month, 1).plus(DatePeriod(months = 1)).minusDays(1).day

/**
 * Adds a duration to a local date time as wall clock time (no time zone transitions applied).
 */
operator fun LocalDateTime.plus(duration: Duration): LocalDateTime =
    toInstant(TimeZone.UTC).plus(duration).toLocalDateTime(TimeZone.UTC)

operator fun LocalDateTime.minus(duration: Duration): LocalDateTime = plus(-duration)

/**
 * The wall clock duration between two local date times.
 */
operator fun LocalDateTime.minus(other: LocalDateTime): Duration =
    toInstant(TimeZone.UTC) - other.toInstant(TimeZone.UTC)

fun LocalTime.toDurationSinceMidnight(): Duration = toNanosecondOfDay().nanoseconds

fun Duration.toLocalTime(): LocalTime = LocalTime.fromNanosecondOfDay(inWholeNanoseconds)

fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

/**
 * The number of days in the month.
 */
fun kotlinx.datetime.Month.length(leapYear: Boolean): Int {
    return when (this) {
        kotlinx.datetime.Month.FEBRUARY -> if (leapYear) 29 else 28
        kotlinx.datetime.Month.APRIL, kotlinx.datetime.Month.JUNE,
        kotlinx.datetime.Month.SEPTEMBER, kotlinx.datetime.Month.NOVEMBER -> 30

        else -> 31
    }
}

/**
 * Adds calendar days in [zone], keeping the wall-clock time. Matches `ZonedDateTime.plusDays`: the
 * current offset is kept when the result is in an overlap and a gap shifts forward by its length.
 */
fun Instant.plusDays(days: Long, zone: TimeZone = systemTimeZone): Instant {
    val local = toLocalDateTime(zone)
    return LocalDateTime(local.date.plusDays(days), local.time).toInstant(zone, zone.offsetAt(this))
}

fun Instant.plusDays(days: Int, zone: TimeZone = systemTimeZone): Instant = plusDays(days.toLong(), zone)

/**
 * Replaces the wall-clock time of day in [zone]. Matches `ZonedDateTime.with(LocalTime)`.
 */
fun Instant.withTime(time: LocalTime, zone: TimeZone = systemTimeZone): Instant =
    LocalDateTime(toLocalDate(zone), time).toInstant(zone, zone.offsetAt(this))

/*
 * java.time.Duration conversions. java floors to the whole second (getSeconds) and then truncates
 * (toMinutes, toHours, toDays), while kotlin's inWhole* truncate toward zero. They only differ for
 * negative durations with a fractional second. toMillis already truncates, like inWholeMilliseconds.
 */
fun Duration.toJavaSeconds(): Long {
    val whole = inWholeSeconds
    return if (this < (whole).seconds) whole - 1 else whole
}

fun Duration.toJavaMinutes(): Long = toJavaSeconds() / 60
fun Duration.toJavaHours(): Long = toJavaSeconds() / 3600
fun Duration.toJavaDays(): Long = toJavaSeconds() / 86400

private const val NANOS_PER_DAY = 86_400_000_000_000L
private const val NANOS_PER_HOUR = 3_600_000_000_000L

/**
 * Converts a local date-time to an instant in [zone], keeping [preferredOffset] when it is valid for that
 * local time (DST overlaps). In a gap the local time is shifted forward by the gap length. This matches
 * java.time `ZonedDateTime.ofLocal(localDateTime, zone, preferredOffset)`.
 */
fun LocalDateTime.toInstant(zone: TimeZone, preferredOffset: UtcOffset): Instant {
    val candidate = toInstant(preferredOffset)
    if (candidate.toLocalDateTime(zone) == this) {
        return candidate
    }
    return toInstant(zone)
}

/**
 * Truncates the wall-clock time in [zone] to a multiple of [step] (e.g. the start of the local hour for 1.hours),
 * keeping the original offset across DST overlaps. Matches java.time `ZonedDateTime.truncatedTo(unit)`:
 * [step] must evenly divide a day.
 */
fun Instant.truncateTo(step: Duration, zone: TimeZone = systemTimeZone): Instant {
    val stepNanos = step.inWholeNanoseconds
    require(stepNanos > 0 && step <= 1.days && NANOS_PER_DAY % stepNanos == 0L) {
        "step must evenly divide a day"
    }
    val local = toLocalDateTime(zone)
    val nanoOfDay = local.time.toNanosecondOfDay()
    val truncated = LocalDateTime(local.date, LocalTime.fromNanosecondOfDay(nanoOfDay / stepNanos * stepNanos))
    return truncated.toInstant(zone, zone.offsetAt(this))
}

/**
 * Rounds half-up to the nearest multiple of [step] within the local hour in [zone], then truncates to the
 * minute. Matches the former `ZonedDateTime.roundNearestMinute(minutes)` (step = minutes).
 */
fun Instant.roundToNearest(step: Duration, zone: TimeZone = systemTimeZone): Instant {
    val stepNanos = step.inWholeNanoseconds
    require(stepNanos > 0) { "step must be positive" }
    val nanoOfHour = toLocalDateTime(zone).time.toNanosecondOfDay() % NANOS_PER_HOUR
    val rounded = (nanoOfHour + stepNanos / 2) / stepNanos * stepNanos
    return (this + (rounded - nanoOfHour).nanoseconds).truncateTo(1.minutes, zone)
}

/**
 * Decodes a date stored with `LocalDate.toString()`. Returns null when nothing is stored, when the value is the
 * java.time `LocalDate.MIN` sentinel ("-999999999-01-01", previously used as "never"), or when it can't be
 * parsed. Callers only compare the result to today, so null keeps the old "never sent" meaning.
 */
fun decodeStoredLocalDate(raw: String?): LocalDate? {
    val value = raw?.trim()
    if (value.isNullOrEmpty() || value == JAVA_LOCAL_DATE_MIN) {
        return null
    }
    return runCatching { LocalDate.parse(value) }.getOrNull()
}

private const val JAVA_LOCAL_DATE_MIN = "-999999999-01-01"
