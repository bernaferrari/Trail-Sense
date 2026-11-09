package com.kylecorry.sol.science.astronomy.locators

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.tests.ut

internal class SunTest {

    private val sun = SunModel()

    @Test
    fun getCoordinates() {
        val coords = sun.getCoordinates(ut(1992, 10, 13, 0, 0))
        assertEquals(-7.785303541399693, coords.declination, 0.0001)
        assertEquals(198.3814040423241, coords.rightAscension, 0.0001)
    }

    @Test
    fun getDistance() {
        val dist = sun.getDistance(ut(2021, 9, 3, 0, 0))
        assertEquals(150915932.659f, dist.value, 1000f)

        val dist2 = sun.getDistance(ut(2015, 2, 15, 0, 0))
        assertEquals(1.478E08f, dist2.value, 100000f)
    }

    @Test
    fun getAngularDiameter() {
        val diameter = sun.getAngularDiameter(ut(2015, 2, 15, 0, 0))
        assertEquals(0.539790, diameter, 0.0001)
    }

    @Test
    fun getMeanAnomaly() {
        val mean = sun.getMeanAnomaly(ut(1992, 10, 13, 0, 0))
        assertEquals(278.99397, mean, 0.0001)
    }
}