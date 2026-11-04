package com.kylecorry.trail_sense.tools.solarpanel

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSolarPanelAlignerTest : ToolTestBase(Tools.SOLAR_PANEL_ALIGNER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.SOLAR_PANEL_ALIGNER)) {
            return
        }

        // It should show the solar panel details for today
        hasText(string(R.string.tilt), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        hasText(Regex("-?\\d+°"))
        hasText(Regex("-?\\d+°"), index = 1)

        hasText(string(R.string.compass_azimuth))
        hasText(Regex("\\d+°"))
        hasText(Regex("\\d+°"), index = 1)

        hasText(Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that today is selected

        // It should show the solar panel details for a custom time
        click(id("solar_now_btn"))

        clickOk()

        hasText(string(R.string.tilt))
        hasText(Regex("-?\\d+°"))
        hasText(Regex("-?\\d+°"), index = 1)

        hasText(string(R.string.compass_azimuth))
        hasText(Regex("\\d+°"))
        hasText(Regex("\\d+°"), index = 1)

        hasText(Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that the now button is selected
        hasText("2h")
    }
}
