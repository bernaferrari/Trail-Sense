package com.kylecorry.sol.math.filters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatPrecisionTest {
    @Test fun computesFloatSpacingAcrossTheRange() {
        assertEquals(Float.fromBits(1), floatUlp(0f))
        assertEquals(Float.fromBits(1), floatUlp(Float.fromBits(1)))
        assertEquals(1.1920929e-7f, floatUlp(1f))
        assertEquals(floatUlp(1f), floatUlp(-1f))
        assertEquals(Float.fromBits(0x73800000), floatUlp(Float.MAX_VALUE))
        assertEquals(Float.POSITIVE_INFINITY, floatUlp(Float.NEGATIVE_INFINITY))
        assertTrue(floatUlp(Float.NaN).isNaN())
    }
}
