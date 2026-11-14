package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.time.Time.atEndOfDayIn
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.astronomy.locators.SunModel
import com.kylecorry.sol.science.astronomy.rst.RobustRiseSetModel
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.EclipticCoordinate
import com.kylecorry.sol.science.astronomy.units.JulianDay
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Instant

internal object SunCalculationModel {
    private val sun = SunModel()

    @Suppress("LongParameterList")
    fun events(
        time: Instant,
        zone: TimeZone,
        location: Coordinate,
        mode: SunTimesMode,
        withRefraction: Boolean,
        withParallax: Boolean
    ): RiseSetTransitTimes {
        val altitude = when (mode) {
            SunTimesMode.Actual -> -0.8333
            SunTimesMode.Civil -> -6.0
            SunTimesMode.Nautical -> -12.0
            SunTimesMode.Astronomical -> -18.0
        }
        return RobustRiseSetModel().calculate(sun, time, zone, location, altitude, withRefraction, withParallax)
    }

    fun position(
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean,
        withParallax: Boolean
    ): CelestialObservation {
        val distance = sun.getDistance(time)
        val horizon = AstroUtils.getLocation(sun, time, location, withRefraction, withParallax, distance)
        return CelestialObservation(
            Bearing.from(horizon.azimuth.toFloat()),
            horizon.altitude.toFloat(),
            sun.getAngularDiameter(time, distance).toFloat(),
            distance = distance
        )
    }

    /**
     * The apparent ecliptic longitude of the sun in degrees.
     */
    fun longitude(time: Instant): Float = eclipticLongitude(time).toFloat()

    /**
     * Gets the next time on or after [today] when the sun reaches the given ecliptic [longitude].
     */
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
            correction = 58 * sinDegrees(longitude - eclipticLongitude(time))
            jd += correction
            iterations++
        } while (correction.absoluteValue > 0.00001 && iterations < 100)
        check(correction.absoluteValue <= 0.00001) { "Solar longitude refinement did not converge" }
        return JulianDay.from(jd)
    }

    /**
     * Gets the night (sunset to sunrise) closest to [time]. During polar night, the whole local day is returned.
     */
    fun closestNight(time: Instant, zone: TimeZone, location: Coordinate, mode: SunTimesMode): Range<Instant>? {
        val yesterday = events(time.plus(-1, DateTimeUnit.DAY, zone), zone, location, mode, false, false)
        val today = events(time, zone, location, mode, false, false)
        val tomorrow = events(time.plus(1, DateTimeUnit.DAY, zone), zone, location, mode, false, false)

        val lastNight = rangeOrNull(yesterday.set, today.rise)
        val tonight = rangeOrNull(today.set, tomorrow.rise)
        if (lastNight == null || tonight == null) {
            return polarNight(time, zone, location, today)
        }

        val isLastNightCloser = (lastNight.end - time).absoluteValue < (tonight.start - time).absoluteValue
        return if (isLastNightCloser) lastNight else tonight
    }

    /**
     * The whole local day if the sun stays down all day, otherwise null.
     */
    private fun polarNight(
        time: Instant,
        zone: TimeZone,
        location: Coordinate,
        today: RiseSetTransitTimes
    ): Range<Instant>? {
        val isPolarNight = today.rise == null && position(time, location, false, false).altitude <= 0
        if (!isPolarNight) {
            return null
        }
        val date = time.toLocalDateTime(zone).date
        return Range(date.atStartOfDayIn(zone), date.atEndOfDayIn(zone))
    }

    private fun rangeOrNull(start: Instant?, end: Instant?): Range<Instant>? {
        return if (start != null && end != null) Range(start, end) else null
    }

    private fun eclipticLongitude(time: Instant): Double {
        return EclipticCoordinate.fromEquatorial(sun.getCoordinates(time), JulianDay.centuries(time)).eclipticLongitude
    }
}
