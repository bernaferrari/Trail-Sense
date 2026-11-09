package com.kylecorry.trail_sense.tools.ruler

import com.kylecorry.trail_sense.test_utils.views.field
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

        // Enter a to ratio. The scale editor is a sheet, which hides test tags, so its fields are found by label.
        click(id("map_scale"))
        click(string(R.string.map_scale_ratio), exact = true)
        input(field(string(R.string.distance_to)), "10")
        hasText(field(string(R.string.distance_from)), "1")
        click(string(R.string.done), exact = true)
        hasText(id("map_distance"), Regex("\\d+\\.\\d+ m"))

        // Switch to verbal scale
        click(id("map_scale"))
        click(string(R.string.map_scale_verbal), exact = true)
        input(field(string(R.string.distance_from)), "1")
        input(field(string(R.string.distance_to)), "10")
        click(string(R.string.done), exact = true)
        hasText(id("map_distance"), Regex("\\d+(\\.\\d+)? ft"))

        // Switch back to ratio and verify it's still there
        click(id("map_scale"))
        click(string(R.string.map_scale_ratio), exact = true)
        hasText(field(string(R.string.distance_from)), "1")
        hasText(field(string(R.string.distance_to)), "10")
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
