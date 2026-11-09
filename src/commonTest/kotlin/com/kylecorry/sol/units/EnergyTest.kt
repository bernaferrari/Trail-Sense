package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals

class EnergyTest {

    @Test
    fun convert() {
        convert(1f, 1, 1, 1f)
        convert(1f, 1, 2, 0.7375621493f)
        convert(2f, 2, 1, 2.7116358966f)
    }

    private fun convert(value: Float, from: Int, to: Int, expected: Float) {
        val energy = Energy.from(value, EnergyUnits.entries.first { it.id == from })
        val converted = energy.convertTo(EnergyUnits.entries.first { it.id == to })
        assertEquals(expected, converted.value, 0.01f)
        assertEquals(to, converted.units.id)
    }

}