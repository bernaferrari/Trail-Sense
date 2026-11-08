package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.astronomy.locators.SunModel
import com.kylecorry.sol.science.astronomy.rst.RobustRiseSetModel
import com.kylecorry.sol.science.astronomy.rst.SearchRiseSetTimes
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.EclipticCoordinate
import com.kylecorry.sol.science.astronomy.units.JulianDay
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.TimeZone
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
}
