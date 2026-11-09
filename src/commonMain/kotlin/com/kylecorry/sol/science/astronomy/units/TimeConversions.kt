@file:Suppress("TooManyFunctions")

package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.time.Time
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val NANOS_PER_SECOND = 1_000_000_000L
private const val NANOS_PER_HOUR = 3_600 * NANOS_PER_SECOND
private const val NANOS_PER_DAY = 24 * NANOS_PER_HOUR
private const val MILLIS_PER_HOUR = 3_600_000.0
private const val DEGREES_PER_HOUR = 15

fun Instant.toJulianDay(includeTime: Boolean = true): Double {
    return JulianDay.of(toLocalDateTime(TimeZone.UTC), includeTime)
}

fun Instant.toJulianCenturies(): Double = JulianDay.centuries(this)

fun fromJulianDay(julianDay: Double): Instant = JulianDay.from(julianDay)

/**
 * The time since midnight.
 */
fun LocalTime.toDuration(): Duration = hour.hours + minute.minutes + second.seconds + nanosecond.nanoseconds

fun LocalTime.toDecimalHours(): Double = toDuration().inWholeNanoseconds / NANOS_PER_HOUR.toDouble()

/**
 * The time of day after this duration has elapsed since midnight, wrapping around whole days.
 */
fun Duration.toLocalTime(): LocalTime {
    val nanos = ((inWholeNanoseconds % NANOS_PER_DAY) + NANOS_PER_DAY) % NANOS_PER_DAY
    val seconds = nanos / NANOS_PER_SECOND
    return LocalTime(
        hour = (seconds / 3600).toInt(),
        minute = ((seconds / 60) % 60).toInt(),
        second = (seconds % 60).toInt(),
        nanosecond = (nanos % NANOS_PER_SECOND).toInt()
    )
}

/**
 * The duration in decimal hours (millisecond precision).
 */
fun Duration.toDecimal(): Double = inWholeMilliseconds / MILLIS_PER_HOUR

fun Duration.toDegrees(): Double = toDecimal() * DEGREES_PER_HOUR

fun Duration.toRadians(): Double = toDegrees().toRadians()

fun degreesToTime(degrees: Double): Duration = Time.hours(degrees / DEGREES_PER_HOUR)

fun dmsToTime(degrees: Int, minutes: Int, seconds: Number): Duration {
    return degreesToTime(degrees + minutes / 60.0 + seconds.toDouble() / 3600.0)
}

fun timeToDecimal(hours: Number, minutes: Number, seconds: Number): Double {
    return hours.toDouble() + minutes.toDouble() / 60.0 + seconds.toDouble() / 3600.0
}

fun timeToAngle(hours: Number, minutes: Number, seconds: Number): Double {
    return timeToDecimal(hours, minutes, seconds) * DEGREES_PER_HOUR
}
