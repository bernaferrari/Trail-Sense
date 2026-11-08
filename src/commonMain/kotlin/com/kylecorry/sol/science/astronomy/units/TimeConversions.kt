@file:Suppress("TooManyFunctions")

package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.time.Time
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun Instant.toJulianDay(includeTime: Boolean = true): Double =
    JulianDay.of(toLocalDateTime(TimeZone.UTC), includeTime)
fun Instant.toJulianCenturies(): Double = JulianDay.centuries(this)
fun fromJulianDay(julianDay: Double): Instant = JulianDay.from(julianDay)
fun LocalTime.toDuration(): Duration = hour.hours + minute.minutes + second.seconds + nanosecond.nanoseconds
fun LocalTime.toDecimalHours(): Double = toDuration().inWholeNanoseconds / 3600000000000.0
fun Duration.toLocalTime(): LocalTime {
    val dayNanos = 86400000000000L
    val nanos = ((inWholeNanoseconds % dayNanos) + dayNanos) % dayNanos
    val second = nanos / 1000000000L
    return LocalTime((second / 3600).toInt(), ((second / 60) % 60).toInt(), (second % 60).toInt(),
        (nanos % 1000000000L).toInt())
}
fun Duration.toDecimal(): Double = inWholeMilliseconds / 3600000.0
fun Duration.toDegrees(): Double = toDecimal() * 15
fun Duration.toRadians(): Double = toDegrees().toRadians()
fun degreesToTime(degrees: Double): Duration = Time.hours(degrees / 15)
fun dmsToTime(degrees: Int, minutes: Int, seconds: Number): Duration =
    degreesToTime(degrees + minutes / 60.0 + seconds.toDouble() / 3600.0)
fun timeToDecimal(hours: Number, minutes: Number, seconds: Number): Double =
    hours.toDouble() + minutes.toDouble() / 60.0 + seconds.toDouble() / 3600.0
fun timeToAngle(hours: Number, minutes: Number, seconds: Number): Double = timeToDecimal(hours, minutes, seconds) * 15
