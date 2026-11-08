@file:Suppress("ComplexCondition")

package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.locators.SunModel
import com.kylecorry.sol.science.astronomy.rst.RobustRiseSetModel
import com.kylecorry.sol.science.astronomy.rst.SearchRiseSetTimes
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.EclipticCoordinate
import com.kylecorry.sol.science.astronomy.units.JulianDay
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Instant

internal object SunCalculationModel {
    private val sun = SunModel()

    fun events(
        time: Instant, zone: TimeZone, location: Coordinate, mode: SunTimesMode,
        withRefraction: Boolean, withParallax: Boolean
    ): SearchRiseSetTimes {
        val altitude = when (mode) {
            SunTimesMode.Actual -> -0.8333
            SunTimesMode.Civil -> -6.0
            SunTimesMode.Nautical -> -12.0
            SunTimesMode.Astronomical -> -18.0
        }
        return RobustRiseSetModel().calculate(sun, time, zone, location, altitude, withRefraction, withParallax)
    }

    fun position(
        time: Instant, location: Coordinate, withRefraction: Boolean, withParallax: Boolean
    ): CelestialObservation {
        val distance = sun.getDistance(time)
        val horizon = AstroUtils.getLocation(sun, time, location, withRefraction, withParallax, distance)
        return CelestialObservation(
            Bearing.from(horizon.azimuth.toFloat()), horizon.altitude.toFloat(),
            sun.getAngularDiameter(time, distance).toFloat(), distance = distance
        )
    }

    fun longitude(time: Instant): Float = EclipticCoordinate.fromEquatorial(
        sun.getCoordinates(time), JulianDay.centuries(time)
    ).eclipticLongitude.toFloat()
    fun nextAtLongitude(longitude: Float, today: Instant, zone: TimeZone): Instant {
        var date = today
        for (i in 0..365) {
            val candidate = today.plus(i, DateTimeUnit.DAY, zone)
            if (deltaAngle(longitude, longitude(candidate)).absoluteValue < 1f) {
                date = candidate
                break
            }
        }
        var jd = JulianDay.of(date.toLocalDateTime(TimeZone.UTC))
        var correction: Double
        var iterations = 0
        do {
            val time = JulianDay.from(jd)
            val solarLongitude = EclipticCoordinate.fromEquatorial(sun.getCoordinates(time), JulianDay.centuries(time))
                .eclipticLongitude
            correction = 58 * sinDegrees(longitude - solarLongitude)
            jd += correction
            iterations++
        } while (correction.absoluteValue > 0.00001 && iterations < 100)
        check(correction.absoluteValue <= 0.00001) { "Solar longitude refinement did not converge" }
        return JulianDay.from(jd)
    }

    fun closestNight(time: Instant, zone: TimeZone, location: Coordinate, mode: SunTimesMode): Range<Instant>? {
        val yesterday = events(time.plus(-1, DateTimeUnit.DAY, zone), zone, location, mode, false, false)
        val today = events(time, zone, location, mode, false, false)
        val tomorrow = events(time.plus(1, DateTimeUnit.DAY, zone), zone, location, mode, false, false)
        if (yesterday.set == null || today.set == null || today.rise == null || tomorrow.rise == null) {
            return if (!(position(time, location, false, false).altitude > 0) && today.rise == null) {
                val date = time.toLocalDateTime(zone).date
                Range(date.atStartOfDayIn(zone), LocalDateTime(date.year, date.month, date.day, 23, 59, 59, 999999999)
                    .toInstant(zone))
            } else null
        }
        val lastNight = Range(yesterday.set, today.rise)
        val tonight = Range(today.set, tomorrow.rise)
        return if ((lastNight.end - time).absoluteValue < (tonight.start - time).absoluteValue) lastNight else tonight
    }

}
