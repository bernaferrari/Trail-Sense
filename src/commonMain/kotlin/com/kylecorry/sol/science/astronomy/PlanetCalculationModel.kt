package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.locators.PlanetPositionModel
import com.kylecorry.sol.science.astronomy.rst.RobustRiseSetModel
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

internal object PlanetCalculationModel {
    fun events(
        planet: Planet, time: Instant, zone: TimeZone, location: Coordinate,
        withRefraction: Boolean, withParallax: Boolean
    ) = RobustRiseSetModel().calculate(
        PlanetPositionModel(planet), time, zone, location, 0.0, withRefraction, withParallax
    )

    fun position(
        planet: Planet, time: Instant, location: Coordinate, withRefraction: Boolean, withParallax: Boolean
    ): CelestialObservation {
        val locator = PlanetPositionModel(planet)
        val horizon = AstroUtils.getLocation(locator, time, location, withRefraction, withParallax)
        return CelestialObservation(
            Bearing.from(horizon.azimuth.toFloat()), horizon.altitude.toFloat(),
            locator.getAngularDiameter(time).toFloat(), locator.getMagnitude(time).toFloat(), locator.getDistance(time)
        )
    }
}
