package com.kylecorry.trail_sense.tools.guide

import com.kylecorry.trail_sense.test_utils.views.view
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolUserGuideTest : ToolTestBase(ToolIds.USER_GUIDE) {

    // The search field is the only text field on the page
    private val searchBox = { view(By.clazz("android.widget.EditText")) }

    @Test
    fun verifyBasicFunctionality() {
        isVisible(searchBox)

        // Verify it shows each guide
        val guides = Guides.guides(context)
            .flatMap { it.guides }
            // TODO: Open each guide, maybe by navigating directly to them
            .take(2)


        guides.forEachIndexed { index, guide ->

            // Skip the weather guide (no good way to differentiate it from the section in this test)
            if (guide.name == "Weather") {
                return@forEachIndexed
            }

            // Wait for the guides to load
            hasText(guides.first().name)

            click(guide.name)

            // Wait for the guide to load
            hasText(guide.name)

            back()
        }

        // Search
        input(searchBox, "Sett")
        hasText("Settings")

        verifyQuickAction()
    }

    fun verifyQuickAction() {
        openQuickActions()

        click(quickAction(ToolIds.QUICK_ACTION_USER_GUIDE))

        // Verify it shows the user guide
        hasText("Tools")
    }
}
