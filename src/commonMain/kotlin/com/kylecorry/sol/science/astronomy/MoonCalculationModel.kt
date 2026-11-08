package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.science.astronomy.locators.MoonPhaseModel
import com.kylecorry.sol.science.astronomy.locators.MoonPositionModel
import com.kylecorry.sol.science.astronomy.rst.RobustRiseSetModel
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

internal object MoonCalculationModel {
    private val moon = MoonPositionModel()
    private val phase = MoonPhaseModel()

    fun events(time: Instant, zone: TimeZone, location: Coordinate, withRefraction: Boolean, withParallax: Boolean) =
        RobustRiseSetModel().calculate(moon, time, zone, location, 0.125, withRefraction, withParallax)

    fun position(
        time: Instant, location: Coordinate, withRefraction: Boolean, withParallax: Boolean
    ): CelestialObservation {
        val distance = moon.getDistance(time)
        val horizon = AstroUtils.getLocation(moon, time, location, withRefraction, withParallax, distance)
        return CelestialObservation(
            Bearing.from(horizon.azimuth.toFloat()), horizon.altitude.toFloat(),
            phase.getAngularDiameter(time, location, distance).toFloat(), distance = distance
        )
    }
}
