package com.kylecorry.trail_sense.tools.ballistics

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.views.option
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolBallisticsTest : ToolTestBase(Tools.BALLISTICS) {

    @Test
    fun verifyBasicFunctionality() {
        scope()
        energy()
    }

    private fun scope(){
        click("Scope")

        click(id("click_amount"))
        // Choosing an option closes the dialog
        click(option("1/4 MOA"))

        input(id("distance_to_target"), "100")

        input(id("miss_distance_x"), "1")
        click(string(R.string.right), exact = true)

        scrollUntil { isVisible(id("miss_direction_y")) }
        input(id("miss_distance_y"), "2")
        click(string(R.string.above), exact = true)

        // The distance is in yards now (it was feet): 1 in at 100 yd is about 1 MOA, so 4 clicks of 1/4 MOA
        hasText(id("distance_to_target"), "yd")
        hasText(id("adjustment_amount"), "Left 4 clicks\nDown 8 clicks")
    }

    private fun energy(){
        click("Energy")

        input(id("bullet_speed"), "750")
        input(id("bullet_weight"), "7.5")

        hasText(id("energy_amount"), "9.4 FPE")
    }

}
