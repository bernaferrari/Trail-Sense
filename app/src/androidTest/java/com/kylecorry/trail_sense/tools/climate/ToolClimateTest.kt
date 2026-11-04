package com.kylecorry.trail_sense.tools.climate

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolClimateTest : ToolTestBase(Tools.CLIMATE) {

    @Test
    fun verifyBasicFunctionality() {
        // The location lives behind one tappable line
        click(id("climate_location"))
        input(id("utm")(), "42, -72")
        input(id("elevation_input")(), "1000")
        click(id("climate_location_done"))

        // Pick a month next to this one (the year card opens centred on it); the hero answers for it
        val current = java.time.LocalDate.now().month
        val neighbor = if (current == java.time.Month.DECEMBER) current.minus(1) else current.plus(1)
        click(id("climate_month_${neighbor.value}"))
        hasText(neighbor.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, java.util.Locale.getDefault()))

        // The climate zone names the page
        hasText(string(R.string.climate_zone_subtitle, string(R.string.climate_continental)))
    }
}
