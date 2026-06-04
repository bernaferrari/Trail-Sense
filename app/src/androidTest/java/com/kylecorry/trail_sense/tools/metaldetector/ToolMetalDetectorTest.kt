package com.kylecorry.trail_sense.tools.metaldetector

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.mute
import com.kylecorry.trail_sense.test_utils.TestUtils.unmute
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolMetalDetectorTest : ToolTestBase(Tools.METAL_DETECTOR) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.METAL_DETECTOR)) {
            return
        }

        // Verify the title
        hasText(Regex("\\d+ uT"))

        // Make sure the chart is visible
        isVisible(R.id.metal_chart)

        // Can turn on high sensitivity
        isChecked(id("high_sensitivity_toggle"), isChecked = false)
        click(id("high_sensitivity_toggle"))
        isChecked(id("high_sensitivity_toggle"))

        // Can adjust threshold (just verify it is set)
        isVisible(id("threshold"))
        hasText("5.0 uT")

        // Can play sound
        val originalVolume = mute()
        click(id("metal_audio_btn"))
        click(id("metal_audio_btn"))
        unmute(originalVolume)

        // Can disable vibration
        val buttonVibrationToggle = id("metal_vibration_btn")
        click(buttonVibrationToggle)
        // TODO: assert button state and vibration subsystem state

    }
}
