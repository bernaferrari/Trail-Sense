package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.science.astronomy.corrections.EclipticObliquity
import com.kylecorry.sol.science.astronomy.corrections.LongitudinalNutation
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toUTCLocal
import com.kylecorry.sol.time.Time.utc
import kotlinx.datetime.toKotlinLocalDateTime
import kotlin.time.toKotlinInstant
import kotlin.time.toJavaInstant
import java.time.ZoneOffset
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.floor

typealias UniversalTime = LocalDateTime


/**
 * Julian centuries since 2000
 */
fun UniversalTime.toJulianCenturies(): Double {
    return (toJulianDay() - JD_2000) / 36525.0
}

fun UniversalTime.toJulianDay(includeTime: Boolean = true): Double =
    JulianDay.of(toKotlinLocalDateTime(), includeTime)

internal fun UniversalTime.jd0(): Double {
    val Y = (year - 1).toDouble()
    val A = floor(Y / 100)
    val B = 2 - A + floor(A / 4)

    return floor(365.25 * (Y + 4716)) + B - 1094.5 // Regular julian day - 1
}

internal fun UniversalTime.toSiderealTime(apparent: Boolean = false): GreenwichSiderealTime =
    SiderealCalculator.at(toInstant(ZoneOffset.UTC).toKotlinInstant(), apparent)

fun ZonedDateTime.toUniversalTime(): UniversalTime {
    return toUTCLocal()
}

fun UniversalTime.atZeroHour(): UniversalTime {
    return toLocalDate().atStartOfDay()
}

fun Instant.toUniversalTime(): UniversalTime {
    return utc().toUniversalTime()
}

fun fromJulianDay(jd: Double): UniversalTime =
    LocalDateTime.ofInstant(JulianDay.from(jd).toJavaInstant(), ZoneOffset.UTC)

fun UniversalTime.toLocal(zone: ZoneId): ZonedDateTime {
    return atZone(ZoneId.of("UTC")).withZoneSameInstant(zone)
}

fun UniversalTime.toInstant(): Instant {
    return atZone(ZoneId.of("UTC")).toInstant()
}

fun ut0hOnDate(date: ZonedDateTime): UniversalTime {
    val localDate = date.toLocalDate()

    for (i in -1..1) {
        val ut0h = date.plusDays(i.toLong()).toUniversalTime().atZeroHour()
        val local0h = ut0h.toLocal(date.zone)
        if (localDate == local0h.toLocalDate()) {
            return ut0h
        }
    }

    return date.toUniversalTime().atZeroHour()
}

fun LocalTime.toDuration(): Duration {
    return Duration.ofHours(hour.toLong()).plusMinutes(minute.toLong())
        .plusSeconds(second.toLong()).plusNanos(nano.toLong())
}

fun LocalTime.toDecimalHours(): Double {
    val hours = hour.toDouble()
    val minutes = minute.toDouble() / 60.0
    val seconds = second.toDouble() / 3600.0
    val nanos = nano.toDouble() / 3600.0 / 1_000_000_000.0
    return hours + minutes + seconds + nanos
}

fun Duration.toLocalTime(): LocalTime {
    return LocalTime.MIN.plus(this)
}

fun Duration.toDecimal(): Double {
    val millis = toMillis()
    val seconds = millis / 1000.0
    val minutes = seconds / 60.0
    return minutes / 60.0
}

fun Duration.toDegrees(): Double {
    return toDecimal() * 15.0
}

fun Duration.toRadians(): Double {
    return toDegrees().toRadians()
}

fun degreesToTime(degrees: Double): Duration {
    return Time.hours(degrees / 15)
}

fun dmsToTime(degrees: Int, minutes: Int, seconds: Number): Duration {
    val d = degrees + minutes / 60.0 + seconds.toDouble() / 3600.0
    return degreesToTime(d)
}

fun timeToAngle(hours: Number, minutes: Number, seconds: Number): Double {
    return timeToDecimal(hours, minutes, seconds) * 15
}

fun timeToDecimal(hours: Number, minutes: Number, seconds: Number): Double {
    return hours.toDouble() + minutes.toDouble() / 60.0 + seconds.toDouble() / 3600.0
}
