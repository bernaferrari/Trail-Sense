package com.kylecorry.trail_sense.tools.level

import java.util.regex.Pattern
import com.kylecorry.trail_sense.test_utils.views.view
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolBubbleLevelTest : ToolTestBase(ToolIds.BUBBLE_LEVEL) {

    @Test
    fun verifyBasicFunctionality() {
        // The level reads its angles out through its content description
        val angles = string(
            R.string.bubble_level_angles,
            "\\d+\\.\\d°",
            "\\d+\\.\\d°",
            "\\d+\\.\\d°"
        )
        isVisible({ view(By.desc(Pattern.compile(angles, Pattern.DOTALL))) })

        // Verify the level visualization is shown (testTag("level") on the Canvas area)
        isVisible("level")
    }
}
