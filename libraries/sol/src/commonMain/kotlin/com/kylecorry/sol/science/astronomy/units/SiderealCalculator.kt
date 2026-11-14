package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.science.astronomy.corrections.EclipticObliquity
import com.kylecorry.sol.science.astronomy.corrections.LongitudinalNutation
import com.kylecorry.sol.time.Time
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.floor
import kotlin.time.Instant

/**
 * Converts between universal and Greenwich sidereal time (Duffett-Smith, Practical Astronomy).
 */
internal object SiderealCalculator {

    fun at(instant: Instant, apparent: Boolean = false): GreenwichSiderealTime {
        val date = instant.toLocalDateTime(TimeZone.UTC)
        val hours = date.hour.toDouble() + date.minute / 60.0 + date.second / 3600.0 +
            date.nanosecond.toDouble() / 3600.0 / 1_000_000_000.0
        val gst = siderealTimeAtZeroHour(date.date) + SIDEREAL_RATE * hours
        if (!apparent) return GreenwichSiderealTime(gst)
        val centuries = JulianDay.centuries(instant)
        val nutation = LongitudinalNutation.getNutationInLongitude(centuries)
        val obliquity = EclipticObliquity.getTrueObliquityOfEcliptic(centuries)
        return GreenwichSiderealTime(gst + nutation * cos(obliquity.toRadians()) / 15)
    }

    /**
     * The universal time on [date] at which the mean Greenwich sidereal time is [gst].
     */
    fun toUniversalTime(gst: GreenwichSiderealTime, date: LocalDate): Instant {
        val t0 = siderealTimeAtZeroHour(date).mod(24.0)
        val hoursSinceZero = (gst.hours - t0).mod(24.0)
        return date.atStartOfDayIn(TimeZone.UTC) + Time.hours(SOLAR_RATE * hoursSinceZero)
    }

    private fun siderealTimeAtZeroHour(date: LocalDate): Double {
        val jd = JulianDay.of(date.atStartOfDayIn(TimeZone.UTC).toLocalDateTime(TimeZone.UTC), false)
        val year = (date.year - 1).toDouble()
        val century = floor(year / 100)
        // Julian day of January 0.0 of the year
        val jd0 = floor(365.25 * (year + 4716)) + 2 - century + floor(century / 4) - 1094.5
        val t = (jd0 - 2415020.0) / 36525.0
        val r = Algebra.polynomial(t, 6.6460656, 2400.051262, 0.00002581)
        val b = 24 - r + 24 * (date.year - 1900)
        return 0.0657098 * (jd - jd0) - b
    }

    private const val SIDEREAL_RATE = 1.002738
    private const val SOLAR_RATE = 0.99727
}

internal fun GreenwichSiderealTime.toUniversalTime(date: LocalDate): Instant =
    SiderealCalculator.toUniversalTime(this, date)
