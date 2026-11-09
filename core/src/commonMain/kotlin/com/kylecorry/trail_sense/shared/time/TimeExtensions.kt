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
import kotlin.time.Instant

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
 * The last representable instant of the day (one nanosecond before the next day starts).
 */
fun LocalDate.atEndOfDay(zone: TimeZone = systemTimeZone): Instant =
    plusDays(1).atStartOfDayIn(zone) - 1.nanoseconds

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

/** Rounds down to a multiple of [step] since the epoch (e.g. the start of the UTC hour for 1.hours). */
fun Instant.truncateTo(step: Duration): Instant {
    val stepMs = step.inWholeMilliseconds
    require(stepMs > 0) { "step must be positive" }
    return Instant.fromEpochMilliseconds(toEpochMilliseconds().floorDiv(stepMs) * stepMs)
}

/** Rounds half-up to the nearest multiple of [step] since the epoch. */
fun Instant.roundToNearest(step: Duration): Instant {
    val stepMs = step.inWholeMilliseconds
    require(stepMs > 0) { "step must be positive" }
    return Instant.fromEpochMilliseconds((toEpochMilliseconds() + stepMs / 2).floorDiv(stepMs) * stepMs)
}
