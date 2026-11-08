package com.kylecorry.sol.science.astronomy.units

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
}
