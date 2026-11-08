package com.kylecorry.sol.science.astronomy.units

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.algebra.Algebra
import com.kylecorry.sol.science.astronomy.corrections.EclipticObliquity
import com.kylecorry.sol.science.astronomy.corrections.LongitudinalNutation
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.cos
import kotlin.math.floor
import kotlin.time.Instant

internal object SiderealCalculator {
    fun at(instant: Instant, apparent: Boolean = false): GreenwichSiderealTime {
        val date = instant.toLocalDateTime(TimeZone.UTC)
        val jd = JulianDay.of(date, false)
        val year = (date.year - 1).toDouble()
        val century = floor(year / 100)
        val jd0 = floor(365.25 * (year + 4716)) + 2 - century + floor(century / 4) - 1094.5
        val t = (jd0 - 2415020.0) / 36525.0
        val r = Algebra.polynomial(t, 6.6460656, 2400.051262, 0.00002581)
        val b = 24 - r + 24 * (date.year - 1900)
        val t0 = 0.0657098 * (jd - jd0) - b
        val hours = date.hour.toDouble() + date.minute / 60.0 + date.second / 3600.0 +
            date.nanosecond.toDouble() / 3600.0 / 1_000_000_000.0
        val gst = t0 + 1.002738 * hours
        if (!apparent) return GreenwichSiderealTime(gst)
        val centuries = JulianDay.centuries(instant)
        val nutation = LongitudinalNutation.getNutationInLongitude(centuries)
        val obliquity = EclipticObliquity.getTrueObliquityOfEcliptic(centuries)
        return GreenwichSiderealTime(gst + nutation * cos(obliquity.toRadians()) / 15)
    }
}
