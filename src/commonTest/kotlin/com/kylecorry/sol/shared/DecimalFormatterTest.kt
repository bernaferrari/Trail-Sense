package com.kylecorry.sol.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class DecimalFormatterTest {

    @Test
    fun format() {
        provideValues().forEach { args -> format(args[0].arg<Number>(), args[1].arg<Int>(), args[2].arg<Boolean>(), args[3].arg<String>()) }
    }

    private fun format(value: Number, places: Int, strict: Boolean, expected: String) {
        assertEquals(expected, DecimalFormatter.format(value, places, strict))
    }

    companion object {
        fun provideValues(): List<List<Any?>> {
            return listOf(
                listOf(1.155, 1, false, "1.2"),
                listOf(1.155, 1, true, "1.2"),
                listOf(1.155, 4, false, "1.155"),
                listOf(1.155, 4, true, "1.1550"),
                listOf(1.155, 0, true, "1"),
                listOf(0.155, 2, true, "0.16"),
                listOf(100.155, 2, true, "100.16"),
                listOf(100, 2, true, "100.00"),
                listOf(100, 2, false, "100"),
                listOf(Double.NaN, 2, true, "-"),
                listOf(Double.NEGATIVE_INFINITY, 2, true, "-"),
                listOf(Double.POSITIVE_INFINITY, 2, true, "-"),
                listOf(0.155, 2, false, "0.16"),
                listOf(0.0, 2, false, "0"),
                listOf(-1.155, 1, true, "-1.2"),
                listOf(-0.001, 2, true, "0.00"),
                listOf(1.0E-7, 8, true, "0.00000010"),
                listOf(1.5E20, 1, false, "150000000000000000000"),
                listOf(12345.678f, 1, true, "12345.7"),
            )
        }
    }

}