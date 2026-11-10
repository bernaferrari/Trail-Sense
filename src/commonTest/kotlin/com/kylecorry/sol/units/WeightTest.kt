package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class WeightTest {
    @Test
    fun convertTo() {
        provideConversions().forEach { args -> convertTo(args[0].arg<Float>(), args[1].arg<WeightUnits>(), args[2].arg<Float>(), args[3].arg<WeightUnits>()) }
    }

    private fun convertTo(weight: Float, weightUnits: WeightUnits, expected: Float, expectedUnits: WeightUnits) {
        val converted = Weight.from(weight, weightUnits).convertTo(expectedUnits)
        assertEquals(expectedUnits, converted.units)
        assertEquals(expected, converted.value, 0.00001f)
    }

    @Test
    fun times() {
        provideTimes().forEach { args -> times(args[0].arg<Float>(), args[1].arg<WeightUnits>(), args[2].arg<Number>(), args[3].arg<Float>(), args[4].arg<WeightUnits>()) }
    }

    private fun times(
        weight: Float,
        units: WeightUnits,
        amount: Number,
        expected: Float,
        expectedUnits: WeightUnits
    ) {
        val multiplied = Weight.from(weight, units) * amount
        assertEquals(expectedUnits, multiplied.units)
        assertEquals(expected, multiplied.value, 0.00001f)
    }

    @Test
    fun add() {
        provideAdd().forEach { args -> add(args[0].arg<Float>(), args[1].arg<WeightUnits>(), args[2].arg<Float>(), args[3].arg<WeightUnits>(), args[4].arg<Float>(), args[5].arg<WeightUnits>()) }
    }

    private fun add(
        weight: Float,
        units: WeightUnits,
        other: Float,
        otherUnits: WeightUnits,
        expected: Float,
        expectedUnits: WeightUnits
    ) {
        val added = Weight.from(weight, units) + Weight.from(other, otherUnits)
        assertEquals(expectedUnits, added.units)
        assertEquals(expected, added.value, 0.00001f)
    }

    companion object {
        fun provideConversions(): List<List<Any?>> {
            return listOf(
                listOf(2f, WeightUnits.Pounds, 0.907185f, WeightUnits.Kilograms),
                listOf(3f, WeightUnits.Kilograms, 3000f, WeightUnits.Grams),
                listOf(4f, WeightUnits.Ounces, 0.25f, WeightUnits.Pounds),
                listOf(4f, WeightUnits.Grams, 0.141096f, WeightUnits.Ounces),
                listOf(4f, WeightUnits.Milligrams, 0.004f, WeightUnits.Grams),
                listOf(1f, WeightUnits.Grains, 64.79891f, WeightUnits.Milligrams),
                listOf(1f, WeightUnits.Grams, 1000f, WeightUnits.Milligrams),
                listOf(1f, WeightUnits.Stone, 6.350293f, WeightUnits.Kilograms),
            )
        }
        fun provideTimes(): List<List<Any?>> {
            return listOf(
                listOf(2f, WeightUnits.Pounds, 2, 4f, WeightUnits.Pounds),
                listOf(3f, WeightUnits.Kilograms, 0.5f, 1.5f, WeightUnits.Kilograms),
                listOf(4f, WeightUnits.Ounces, -3, 12f, WeightUnits.Ounces)
            )
        }
        fun provideAdd(): List<List<Any?>> {
            return listOf(
                listOf(2f, WeightUnits.Pounds, 2f, WeightUnits.Pounds, 4f, WeightUnits.Pounds),
                listOf(3f, WeightUnits.Kilograms, 500f, WeightUnits.Grams, 3.5f, WeightUnits.Kilograms),
                listOf(500f, WeightUnits.Grams, 3f, WeightUnits.Kilograms, 3500f, WeightUnits.Grams),
            )
        }

    }
}