package com.kylecorry.trail_sense.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DecimalInputTest {

    @Test
    fun eitherDecimalSeparatorIsAccepted() {
        assertEquals(2.5f, "2.5".toDecimalInputOrNull())
        assertEquals(2.5f, "2,5".toDecimalInputOrNull())
        assertEquals(-0.25f, "-0,25".toDecimalInputOrNull())
        assertEquals(12f, " 12 ".toDecimalInputOrNull())
    }

    @Test
    fun unfinishedOrNonNumericTextIsNull() {
        assertNull("".toDecimalInputOrNull())
        assertNull("-".toDecimalInputOrNull())
        assertNull(",".toDecimalInputOrNull())
        assertNull("1,2,3".toDecimalInputOrNull())
        assertNull("abc".toDecimalInputOrNull())
        assertNull("NaN".toDecimalInputOrNull())
        assertNull("Infinity".toDecimalInputOrNull())
    }
}
