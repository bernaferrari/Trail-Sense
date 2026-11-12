package com.kylecorry.trail_sense.shared.extensions

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolExtensionsTest {

    @Test
    fun rangeIsTheMinimumToTheMaximum() {
        assertEquals(Range(-2, 7), listOf(3, 7, -2, 0).range())
        assertNull(emptyList<Int>().range())
    }

    @Test
    fun valuesBetweenAreInclusiveMultiplesOfTheDivisor() {
        assertEquals(listOf(10f, 20f, 30f), getValuesBetween(5f, 30f, 10f))
        assertEquals(listOf(0f, 0.5f, 1f), getValuesBetween(0f, 1f, 0.5f))
        assertEquals(emptyList(), getValuesBetween(1f, 4f, 5f))
    }

    @Test
    fun boundsCoverEveryGeofence() {
        val bounds = CoordinateBounds.from(
            listOf(
                Geofence(Coordinate(10.0, 20.0), Distance.meters(1000f)),
                Geofence(Coordinate(-5.0, 30.0), Distance.meters(1000f))
            )
        )

        assertTrue(bounds.north > 10.0 && bounds.north < 10.1)
        assertTrue(bounds.south < -5.0 && bounds.south > -5.1)
        assertTrue(bounds.west < 20.0 && bounds.west > 19.9)
        assertTrue(bounds.east > 30.0 && bounds.east < 30.1)
    }
}
