package com.kylecorry.trail_sense.tools.convert.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleConverterPageTest {

    @Test
    fun plainValuesHaveAtMostFourDecimals() {
        assertEquals("1", plain(1f))
        assertEquals("3.2808", plain(3.28084f))
        assertEquals("0.1", plain(0.1f))
        assertEquals("100", plain(100f))
        assertEquals("0.0001", plain(0.00006f))
        // 0.00005f is slightly under 0.00005 as a float, so it rounds down
        assertEquals("0", plain(0.00005f))
        assertEquals("-2.5", plain(-2.5f))
        assertEquals("0", plain(-0.00001f))
        assertEquals("0", plain(0f))
        assertEquals("", plain(Float.NaN))
        assertEquals("", plain(Float.POSITIVE_INFINITY))
    }

    @Test
    fun hugeValuesAreWrittenOutWithoutAnExponent() {
        // 1e15f is really 999999986991104 as a float
        assertEquals("999999986991104", plain(1e15f))
        val huge = plain(-3e38f)
        assertEquals('-', huge.first())
        assertEquals(39, huge.length - 1)
        assertTrue(huge.drop(1).all { it.isDigit() })
    }

    @Test
    fun symbolIsWhatIsLeftOfTheFormattedAmount() {
        assertEquals("ft", symbol("1 ft", "1"))
        assertEquals("", symbol("one foot", "1"))
    }
}
