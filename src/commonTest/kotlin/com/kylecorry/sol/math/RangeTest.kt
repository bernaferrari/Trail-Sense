package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

internal class RangeTest {

    @Test
    fun contains() {
        assertTrue(Range(1f, 2f).contains(1.5f))
        assertTrue(Range(1f, 2f).contains(1f))
        assertTrue(Range(1f, 2f).contains(2f))
        assertFalse(Range(1f, 2f).contains(0f))
        assertFalse(Range(1f, 2f).contains(3f))
    }

    @Test
    fun clamp() {
        assertEquals(1.5f, Range(1f, 2f).clamp(1.5f))
        assertEquals(2f, Range(1f, 2f).clamp(2.5f))
        assertEquals(1f, Range(1f, 2f).clamp(0.5f))
    }

    @Test
    fun intersection() {
        intersection(1f, 2f, 1f, 2f, 1f, 2f)
        intersection(1f, 2f, 1f, 1.5f, 1f, 1.5f)
        intersection(1f, 2f, 1.5f, 2f, 1.5f, 2f)
        intersection(1f, 2f, 1.5f, 1.5f, 1.5f, 1.5f)
        intersection(1f, 2f, 0f, 1f, 1f, 1f)
        intersection(1f, 2f, 2f, 3f, 2f, 2f)
        intersection(1f, 2f, 0f, 3f, 1f, 2f)
        intersection(1f, 2f, 0f, 0f, null, null)
        intersection(1f, 2f, 3f, 3f, null, null)
    }

    private fun intersection(
        start: Float,
        end: Float,
        otherStart: Float,
        otherEnd: Float,
        expectedStart: Float?,
        expectedEnd: Float?
    ) {
        val range = Range(start, end)
        val other = Range(otherStart, otherEnd)
        val intersection = range.intersection(other)
        if (expectedStart == null) {
            assertNull(intersection)
        } else {
            assertNotNull(intersection)
            assertEquals(expectedStart, intersection!!.start)
            assertEquals(expectedEnd, intersection.end)
        }
    }
}