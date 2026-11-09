package com.kylecorry.trail_sense.tools.light

import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolLightMeterTest : ToolTestBase(Tools.LIGHT_METER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(TestUtils.context, Tools.LIGHT_METER)) {
            return
        }

        hasText(id("light_title"), Regex("\\d+ lx"))

        // Enter the distance
        input(R.id.amount, "10")

        // The candella and beam distance should be shown
        hasText(id("light_title"), Regex("\\d+ cd"))
        hasText(R.id.beam_distance_text, Regex("\\d+ ft beam distance"))

        // Reset
        click(R.id.reset_btn)
        hasText(id("light_title"), Regex("\\d+ cd"))
        hasText(R.id.beam_distance_text, Regex("\\d+ ft beam distance"))
    }
}
