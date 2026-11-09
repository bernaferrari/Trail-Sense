package com.kylecorry.sol.science.astronomy.units

import kotlin.test.Test
import kotlin.test.assertEquals

internal class EquatorialCoordinateTest {

    @Test
    fun getHourAngle() {
        val coord = EquatorialCoordinate.fromRightAscensionHours(0.0, 3.401667)
        val hourAngle = coord.getHourAngle(GreenwichSiderealTime(18.0))
        assertEquals(14.598333, hourAngle, 0.000001)
    }

    @Test
    fun fromHourAngle(){
        val sidereal = GreenwichSiderealTime(21.0)
        val coord = EquatorialCoordinate.fromHourAngle(0.0, 1.25, sidereal)
        assertEquals(19.75, coord.rightAscensionHours, 0.00001)
    }
}