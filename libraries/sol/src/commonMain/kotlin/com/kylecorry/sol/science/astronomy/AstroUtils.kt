package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.tanDegrees
import com.kylecorry.sol.science.astronomy.locators.ICelestialLocator
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.HorizonCoordinate
import kotlin.time.Instant
import com.kylecorry.sol.science.astronomy.units.SiderealCalculator
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.math.atan2

// Algorithms from Jean Meeus (Astronomical Algorithms 2nd Edition)
internal object AstroUtils {

    fun getAltitude(
        locator: ICelestialLocator,
        ut: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): Float {
        return getLocation(
            locator.getCoordinates(ut),
            ut,
            location,
            withRefraction,
            if (withParallax) locator.getDistance(ut) else null
        ).altitude.toFloat()
    }

    fun getAltitude(
        coordinate: EquatorialCoordinate,
        ut: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        distanceToBody: Distance? = null
    ): Float {
        return getLocation(coordinate, ut, location, withRefraction, distanceToBody).altitude.toFloat()
    }

    fun getAzimuth(
        locator: ICelestialLocator,
        ut: Instant,
        location: Coordinate,
        withParallax: Boolean = false
    ): Bearing {
        val azimuth = getLocation(
            locator.getCoordinates(ut),
            ut,
            location,
            false,
            if (withParallax) locator.getDistance(ut) else null
        ).azimuth.toFloat()
        return Bearing.from(azimuth)
    }

    @Suppress("LongParameterList")
    fun getLocation(
        locator: ICelestialLocator,
        ut: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false,
        distance: Distance? = null
    ): HorizonCoordinate {
        val coords = locator.getCoordinates(ut)
        val parallaxDistance = if (withParallax) {
            distance ?: locator.getDistance(ut)
        } else {
            null
        }
        return getLocation(
            coords,
            ut,
            location,
            withRefraction,
            parallaxDistance
        )
    }

    fun getParallacticAngle(
        locator: ICelestialLocator,
        ut: Instant,
        location: Coordinate
    ): Float {
        val coords = locator.getCoordinates(ut)
        val hourAngle = coords.getHourAngle(SiderealCalculator.at(ut).atLongitude(location.longitude)) * 15
        val latitude = location.latitude
        val declination = coords.declination
        val q = atan2(
            sinDegrees(hourAngle),
            (tanDegrees(latitude) * cosDegrees(declination) - sinDegrees(declination) * cosDegrees(
                hourAngle
            ))
        ).toDegrees()
        return q.toFloat()
    }

    private fun getLocation(
        coordinate: EquatorialCoordinate,
        ut: Instant,
        location: Coordinate,
        withRefraction: Boolean = false,
        distanceToBody: Distance? = null
    ): HorizonCoordinate {
        val siderealTime = SiderealCalculator.at(ut).atLongitude(location.longitude)
        val horizon = if (distanceToBody != null) {
            HorizonCoordinate.fromEquatorial(coordinate, siderealTime, location.latitude, distanceToBody)
        } else {
            HorizonCoordinate.fromEquatorial(coordinate, siderealTime, location.latitude)
        }
        return if (withRefraction) {
            horizon.withRefraction()
        } else {
            horizon
        }
    }
}
