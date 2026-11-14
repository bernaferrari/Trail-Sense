package com.kylecorry.sol.science.astronomy.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class HorizonVectorsCommonTest {
    @Test
    fun roundTripsHorizonCoordinates() {
        val vector = HorizonVectors.toVector(45.0, 30.0)
        val (azimuth, altitude) = HorizonVectors.fromVector(vector)
        assertEquals(45.0, azimuth, 1e-9)
        assertEquals(30.0, altitude, 1e-9)
    }
}
