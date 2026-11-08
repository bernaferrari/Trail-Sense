package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.AstroUtils
import com.kylecorry.sol.science.astronomy.sun.SolarRadiationCalculator
import com.kylecorry.sol.science.astronomy.units.SiderealCalculator
import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SunModelCommonTest {
    @Test
    fun calculatesSolarRadiation() {
        val calculator = SolarRadiationCalculator()
        assertTrue(calculator.getRadiation(Instant.parse("2025-03-20T12:00:00Z"), Coordinate.zero) > 0.5)
        assertEquals(0.0, calculator.getRadiation(Instant.parse("2025-03-20T00:00:00Z"), Coordinate.zero))
    }

    @Test
    fun calculatesSiderealTimeAndPlanetHorizonPosition() {
        val sidereal = SiderealCalculator.at(Instant.parse("2010-02-07T23:30:00Z"))
        assertEquals(8.698091, sidereal.hours, 0.0000005)
        val horizon = AstroUtils.getLocation(
            PlanetPositionModel(Planet.Jupiter), Instant.parse("2025-07-21T07:00:00Z"),
            Coordinate(41.97640, -71.65340), withRefraction = true, withParallax = true
        )
        assertEquals(47.614472, horizon.azimuth, 0.5)
        assertEquals(-8.95975, horizon.altitude, 0.4)
    }

    @Test
    fun calculatesPlanetDistance() {
        val distance = PlanetPositionModel(Planet.Jupiter).getDistance(Instant.parse("2025-07-21T07:00:00Z"))
        assertEquals(6.10, distance.value / 149597871.0, 0.1)
    }

    @Test
    fun calculatesLunarCoordinates() {
        val coordinates = MoonPositionModel().getCoordinates(Instant.parse("1992-04-12T00:00:00Z"))
        assertEquals(13.7652847499, coordinates.declination, 0.001)
        assertEquals(134.6971088, coordinates.rightAscension, 0.001)
    }

    @Test
    fun calculatesCoordinatesAndDistance() {
        val sun = SunModel()
        val coordinates = sun.getCoordinates(Instant.parse("1992-10-13T00:00:00Z"))
        assertEquals(-7.785303541399693, coordinates.declination, 0.0001)
        assertEquals(198.3814040423241, coordinates.rightAscension, 0.0001)
        assertEquals(150915932.659f, sun.getDistance(Instant.parse("2021-09-03T00:00:00Z")).value, 1000f)
    }
}
