package com.kylecorry.sol.science.astronomy.locators

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.tests.ut

internal class MoonTest {

    private val moon = MoonPhaseModel()

    @Test
    fun getAngularDiameter() {
        val diameter = moon.getAngularDiameter(ut(2015, 2, 15, 0, 0))
        assertEquals(0.529422, diameter, 0.0001)
    }

}