package com.kylecorry.trail_sense.tools.light

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasDataPoint
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolLightMeterTest : ToolTestBase(Tools.LIGHT_METER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(TestUtils.context, Tools.LIGHT_METER)) {
            return
        }

        hasText(id("light_title"), Regex("\\d+ lx"))

        // Open the flashlight test and enter the distance
        click(id("light_test_flashlight"))
        input(id("beam_distance"), "10", closeKeyboardOnCompletion = true)

        // The candela and beam distance should be shown
        hasDataPoint(Regex("\\d+ cd"), string(R.string.light_meter_intensity))
        hasDataPoint(Regex("[0-9,.]+ ft"), string(R.string.light_meter_reach))
        isVisible("light_chart")

        // Reset the peak, if the sensor saw any light
        optional {
            click(id("light_peak"))
        }
        hasDataPoint(Regex("\\d+ cd"), string(R.string.light_meter_intensity))
    }
}
