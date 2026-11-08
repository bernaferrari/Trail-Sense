package com.kylecorry.sol.science.astronomy.locators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SunModelCommonTest {
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
