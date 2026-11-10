package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals

internal class DistanceTest {

    @Test
    fun convertTo() {
        convertTo(1, 1f, 6, 0.00001f)
        convertTo(1, 1f, 9, 10f)
        convertTo(10, 0.5f, 9, 12.7f)
    }

    private fun convertTo(fromId: Int, fromValue: Float, toId: Int, expected: Float) {
        val distance = Distance.from(fromValue, DistanceUnits.entries.first { it.id == fromId })
        val converted = distance.convertTo(DistanceUnits.entries.first { it.id == toId })
        assertEquals(expected, converted.value, 0.00001f)
    }

    @Test
    fun times() {
        val multiplied = Distance.meters(10f) * 2f
        assertEquals(DistanceUnits.Meters, multiplied.units)
        assertEquals(20f, multiplied.value, 0.00001f)
    }
}
