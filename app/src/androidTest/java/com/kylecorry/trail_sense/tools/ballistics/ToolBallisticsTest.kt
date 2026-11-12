package com.kylecorry.trail_sense.tools.ballistics

import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.test_utils.byResName
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.views.option
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolBallisticsTest : ToolTestBase(ToolIds.BALLISTICS) {

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

        input(id("miss_distance_x"), "1", closeKeyboardOnCompletion = true)
        // The adjustment readout also says "Right", so pick the option inside the windage selector
        click({ view(By.text(string(R.string.right)).hasAncestor(byResName("miss_direction_x"))) })

        scrollUntil { isVisible(id("miss_direction_y")) }
        input(id("miss_distance_y"), "2", closeKeyboardOnCompletion = true)
        click({ view(By.text(string(R.string.above)).hasAncestor(byResName("miss_direction_y"))) })

        // The distance is in yards now (it was feet): 1 in at 100 yd is about 1 MOA, so 4 clicks of 1/4 MOA
        hasText(id("distance_to_target"), "yd")
        // The adjustment is read out as one description over the drawn numbers
        isVisible(desc("Left 4 clicks\nDown 8 clicks"))
    }

    private fun energy(){
        // The scope page scrolled the choices out of view
        scrollToStart()
        click("Energy")

        input(id("bullet_speed"), "750")
        input(id("bullet_weight"), "7.5")

        hasText(id("energy_amount"), "9.4 FPE")
    }

}
