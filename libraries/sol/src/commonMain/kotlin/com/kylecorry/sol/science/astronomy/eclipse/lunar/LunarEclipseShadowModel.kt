package com.kylecorry.sol.science.astronomy.eclipse.lunar

import com.kylecorry.sol.math.trigonometry.Trigonometry.normalizeAngle
import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.science.astronomy.locators.MoonPhaseModel
import com.kylecorry.sol.science.astronomy.locators.MoonPositionModel
import com.kylecorry.sol.science.astronomy.locators.SunModel
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.HorizonCoordinate
import com.kylecorry.sol.science.astronomy.units.SiderealCalculator
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

internal class LunarEclipseShadowModel {

    private val sun = SunModel()
    private val moon = MoonPositionModel()
    private val phase = MoonPhaseModel()
    private val provider = LunarEclipseParameterModel()

    fun getPeakShadow(
        time: Instant,
        zone: TimeZone,
        location: Coordinate,
        withRefraction: Boolean = false,
        withParallax: Boolean = false
    ): LunarEclipseShadow {
        val ut = time
        val sunCoordinates = sun.getCoordinates(ut)
        val shadowCoordinates = getShadowCoordinates(sunCoordinates)
        val moonDistance = moon.getDistance(ut)
        val shadowPosition = getShadowPosition(
            shadowCoordinates,
            time,
            location,
            withRefraction,
            withParallax,
            moonDistance
        )

        val parameters = provider.getNextLunarEclipseModelParameters(time.plus(-20, DateTimeUnit.DAY, zone))
        val moonDiameter = MOON_DIAMETER_IN_EARTH_RADII
        val umbraRadius =
            LUNAR_ECLIPSE_UMBRA_OUTER_CONTACT_RADIUS -
                parameters.umbralConeRadius -
                (MOON_DIAMETER_IN_EARTH_RADII / 2)
        val penumbraRadius =
            LUNAR_ECLIPSE_PENUMBRA_OUTER_CONTACT_RADIUS +
                parameters.umbralConeRadius -
                (MOON_DIAMETER_IN_EARTH_RADII / 2)
        val umbraDiameter = (2 * umbraRadius).coerceAtLeast(0.0)
        val penumbraDiameter = (2 * penumbraRadius).coerceAtLeast(0.0)
        val moonAngularDiameter = if (withParallax) {
            phase.getAngularDiameter(ut, location, moonDistance)
        } else {
            phase.getAngularDiameter(ut)
        }.toFloat()
        val umbraAngularDiameter = umbraDiameter / moonDiameter * moonAngularDiameter
        val penumbraAngularDiameter = penumbraDiameter / moonDiameter * moonAngularDiameter

        return LunarEclipseShadow(
            umbra = getShadowObservation(shadowPosition, umbraAngularDiameter.toFloat()),
            penumbra = getShadowObservation(shadowPosition, penumbraAngularDiameter.toFloat())
        )
    }

    @Suppress("LongParameterList")
    private fun getShadowPosition(
        shadowCoordinates: EquatorialCoordinate,
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean,
        withParallax: Boolean,
        distanceToMoon: Distance
    ): HorizonCoordinate {
        val ut = time
        val siderealTime = SiderealCalculator.at(ut).atLongitude(location.longitude)
        val position = if (withParallax) {
            HorizonCoordinate.fromEquatorial(shadowCoordinates, siderealTime, location.latitude, distanceToMoon)
        } else {
            HorizonCoordinate.fromEquatorial(shadowCoordinates, siderealTime, location.latitude)
        }

        return if (withRefraction) {
            position.withRefraction()
        } else {
            position
        }
    }

    private fun getShadowCoordinates(sunCoordinates: EquatorialCoordinate): EquatorialCoordinate {
        return EquatorialCoordinate(
            -sunCoordinates.declination,
            normalizeAngle(sunCoordinates.rightAscension + 180)
        )
    }

    private fun getShadowObservation(
        position: HorizonCoordinate,
        angularDiameter: Float
    ): CelestialObservation {
        return CelestialObservation(
            Bearing.from(position.azimuth.toFloat()),
            position.altitude.toFloat(),
            angularDiameter
        )
    }
    private companion object {
        const val MOON_DIAMETER_IN_EARTH_RADII = 0.545
    }

}
