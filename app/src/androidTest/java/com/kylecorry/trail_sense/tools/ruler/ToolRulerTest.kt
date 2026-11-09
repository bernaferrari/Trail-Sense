package com.kylecorry.trail_sense.tools.ruler

import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolRulerTest : ToolTestBase(Tools.RULER) {
    @Test
    fun verifyBasicFunctionality() {
        click(id("ruler"))

        // Verify amount
        hasText(id("ruler_unit_btn"), "in")
        hasText(id("measurement"), Regex("\\d+\\.\\d+ in"))

        // Switch to CM
        // The unit is a two way choice, so pick the option itself
        click("cm", exact = true)
        hasText(id("ruler_unit_btn"), "cm")
        hasText(id("measurement"), Regex("\\d+\\.\\d+ cm"))

        // Enter a to ratio
        click(id("map_scale"))
        click(string(R.string.map_scale_ratio), exact = true)
        input(id("fractional_map_to"), "10")
        hasText(id("fractional_map_from"), "1")
        click(string(R.string.done), exact = true)
        hasText(id("map_distance"), Regex("\\d+\\.\\d+ m"))

        // Switch to verbal scale
        click(id("map_scale"))
        click(string(R.string.map_scale_verbal), exact = true)
        input(id("verbal_map_scale_from"), "1")
        input(id("verbal_map_scale_to"), "10")
        click(string(R.string.done), exact = true)
        hasText(id("map_distance"), Regex("\\d+(\\.\\d+)? ft"))

        // Switch back to ratio and verify it's still there
        click(id("map_scale"))
        click(string(R.string.map_scale_ratio), exact = true)
        hasText(id("fractional_map_from"), "1")
        hasText(id("fractional_map_to"), "10")
        click(string(R.string.done), exact = true)
        hasText(id("map_distance"), Regex("\\d+\\.\\d+ m"))

        verifyQuickActions()
    }

    private fun verifyQuickActions() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_RULER))
        click(quickAction(Tools.QUICK_ACTION_RULER))
        TestUtils.closeQuickActions()
    }
}
