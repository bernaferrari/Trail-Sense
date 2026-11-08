package com.kylecorry.sol.science.astronomy.units

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.math.floor
import kotlin.time.Instant

internal object JulianDay {
    fun of(date: LocalDateTime, includeTime: Boolean = true): Double {
        var year = date.year.toDouble()
        var month = (date.month.ordinal + 1).toDouble()
        val decimalHours = date.hour.toDouble() + date.minute / 60.0 + date.second / 3600.0 +
            date.nanosecond.toDouble() / 3600.0 / 1_000_000_000.0
        val day = date.day.toDouble() + if (includeTime) decimalHours / 24.0 else 0.0
        if (month <= 2) {
            year--
            month += 12
        }
        val century = floor(year / 100)
        val correction = 2 - century + floor(century / 4)
        return floor(365.25 * (year + 4716)) + floor(30.6001 * (month + 1)) + day + correction - 1524.5
    }

    fun centuries(instant: Instant): Double = (of(instant.toLocalDateTime(TimeZone.UTC)) - JD_2000) / 36525.0

    fun from(jd: Double): Instant {
        val f = (jd + 0.5) % 1
        val z = (jd + 0.5) - f

        val a = if (z < 2299161) {
            z
        } else {
            val alpha = floor((z - 1867216.25) / 36524.25)
            z + 1 + alpha - floor(alpha / 4)

    }

    val b = a + 1524
    val c = floor((b - 122.1) / 365.25)
    val d = floor(365.25 * c)
    val e = floor((b - d) / 30.6001)
    val day = b - d - floor(30.6001 * e) + f

    val dayOfMonth = floor(day).toInt()
    val hours = (day - dayOfMonth) * 24
    val hour = floor(hours).toInt()
    val minutes = (hours - hour) * 60
    val minute = floor(minutes).toInt()
    val seconds = floor((minutes - minute) * 60).toInt()
    val month = if (e < 14) {
        e - 1
    } else {
        e - 13
    }.toInt()

    val year = if (month > 2) {
        c - 4716
    } else {
        c - 4715
    }.toInt()

    return LocalDateTime(year, month, dayOfMonth, hour, minute, seconds).toInstant(TimeZone.UTC)

    }

}
