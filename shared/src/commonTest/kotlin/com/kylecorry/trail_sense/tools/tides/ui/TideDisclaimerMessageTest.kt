package com.kylecorry.trail_sense.tools.tides.ui

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class TideDisclaimerMessageTest {

    @Test
    fun onlyTheInstructionsAreBold() {
        val message = tideDisclaimerMessage("Disclaimer", "Instructions", "Tables")

        assertEquals("Disclaimer\n\nInstructions\n\nTables", message.text)
        val bold = message.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, bold.size)
        assertEquals("Instructions", message.text.substring(bold[0].start, bold[0].end))
    }
}
