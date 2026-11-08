package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.science.astronomy.eclipse.lunar.LunarEclipseParameterModel
import com.kylecorry.sol.science.astronomy.eclipse.solar.SolarEclipseParameterModel
import com.kylecorry.sol.science.astronomy.AstroUtils
import com.kylecorry.sol.science.astronomy.moon.MoonPhaseTimeCalculator
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.science.astronomy.sun.SolarRadiationCalculator
import com.kylecorry.sol.science.astronomy.units.SiderealCalculator
import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes

class SunModelCommonTest {
    @Test
    fun calculatesEclipseTimes() {
        val solar = SolarEclipseParameterModel().getNextSolarEclipseModelParameters(Instant.parse("2024-01-01T00:00:00Z"))
        val lunar = LunarEclipseParameterModel().getNextLunarEclipseModelParameters(Instant.parse("2022-09-01T00:00:00Z"))
        assertTrue((solar.maximum - Instant.parse("2024-04-08T18:18:00Z")).absoluteValue < 30.minutes)
        assertTrue((lunar.maximum - Instant.parse("2022-11-08T11:00:00Z")).absoluteValue < 30.minutes)
    }

    @Test
    fun calculatesNewMoonAndStarPosition() {
        val newMoon = MoonPhaseTimeCalculator.getNewMoon(0.0)
        assertTrue((newMoon - Instant.parse("2000-01-06T18:14:00Z")).absoluteValue < 5.minutes)
        assertEquals(MoonTruePhase.New, MoonPhaseModel().getPhase(newMoon).phase)
        val star = STAR_CATALOG.first()
        val coordinates = StarPositionModel(star).getCoordinates(Instant.parse("2000-01-01T12:00:00Z"))
        assertEquals(29.090431118059698, coordinates.declination, 0.000001)
        assertEquals(2.0969161856756404, coordinates.rightAscension, 0.000001)
    }

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
