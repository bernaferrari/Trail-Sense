package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RangeCommonTest {
    @Test
    fun intersectionAndClamp() {
        val range = Range(2, 8)
        assertTrue(5 in range)
        assertEquals(2, range.clamp(0))
        assertEquals(Range(5, 8), range.intersection(Range(5, 10)))
        assertNull(range.intersection(Range(9, 12)))
    }
}
