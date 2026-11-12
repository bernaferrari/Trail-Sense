package com.kylecorry.trail_sense.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidStringFormatTest {

    @Test
    fun fillsPlaceholdersInOrder() {
        assertEquals("5 m", AndroidStringFormat.format("%s m", "5"))
        assertEquals("1 - 2", AndroidStringFormat.format("%s - %s", "1", "2"))
        assertEquals("3 days", AndroidStringFormat.format("%d days", 3))
        assertEquals("10%", AndroidStringFormat.format("%s%%", "10"))
    }

    @Test
    fun fillsPositionalPlaceholders() {
        // Translations may reorder arguments
        assertEquals("hPa 1013", AndroidStringFormat.format("%2\$s %1\$s", "1013", "hPa"))
    }

    @Test
    fun keepsNoBreakSpacesAndText() {
        assertEquals("Actif\u00A0: oui", AndroidStringFormat.format("Actif\u00A0: %s", "oui"))
        assertEquals("plain", AndroidStringFormat.format("plain"))
    }

    @Test
    fun leavesWhatItCannotFill() {
        assertEquals("null and x", AndroidStringFormat.format("%s and %s", null, "x"))
        assertEquals("a %s", AndroidStringFormat.format("%s %s", "a"))
    }

    @Test
    fun formatsDecimals() {
        assertEquals("1.50", AndroidStringFormat.format("%.2f", 1.5).replace(',', '.'))
    }
}
