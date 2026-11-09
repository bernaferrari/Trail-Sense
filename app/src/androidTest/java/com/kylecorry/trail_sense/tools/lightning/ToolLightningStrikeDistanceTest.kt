package com.kylecorry.trail_sense.tools.lightning

import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolLightningStrikeDistanceTest : ToolTestBase(Tools.LIGHTNING_STRIKE_DISTANCE) {

    @Test
    fun verifyBasicFunctionality() {
        // Verify it is ready to record lightning
        hasText(id("start_btn"), string(R.string.lightning))

        // Click the start button
        click(id("start_btn"))

        // Verify it is recording
        hasText(id("lightning_title"), Regex("([0-9.]+) (ft|mi)"))
        hasText(id("start_btn"), string(R.string.thunder))

        // Click the start button again
        click(id("start_btn"))

        // Verify it recorded the distance
        var previousDistance: String? = null
        hasText(id("lightning_title")) {
            val regex = Regex("([0-9.]+) (ft|mi)")
            if (regex.matches(it)) {
                previousDistance = it
                true
            } else {
                false
            }
        }
        hasText(id("start_btn"), string(R.string.lightning))
        hasText(id("lightning_title"), string(R.string.lightning_dangerously_close))

        // Run it again and verify the last distance shows at the bottom of the screen
        click(id("start_btn"))
        click(id("start_btn"))

        hasText(
            R.id.previous_strike,
            Regex("Last strike at [0-9]+:[0-9]+ [AP]M - $previousDistance")
        )
    }
}
