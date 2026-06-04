package com.kylecorry.trail_sense.tools.survival_guide

import android.os.Build
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolSurvivalGuideTest : ToolTestBase(Tools.SURVIVAL_GUIDE) {
    @Test
    fun verifyBasicFunctionality() {
        // Accept the disclaimer
        clickOk()

        isVisible(R.id.list)

        // Verify it shows each chapter
        val chapters = Chapters.getChapters(context)
        chapters.forEachIndexed { index, chapter ->
            scrollUntil {
                hasText(chapter.title)
            }

            click(chapter.title)

            // Wait for the chapter to load
            hasText(chapter.title, ignoreCase = true)
            click("Be prepared")

            back()
        }

        // Search
        input(id("search")(), "Eating fish")
        hasText("fishing gear")
        click("Fish")

        hasText("Food")
        hasText("Fish")

        back()

        verifyQuickAction()
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SURVIVAL_GUIDE))

        input(id("search")(), "Eating fish")
        back(false)
        hasText("fishing gear")
        click("Fish")

        hasText("Food")

        back()

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_SURVIVAL_GUIDE))

        input(id("search")(), "Eating fish")
        if (Build.VERSION.SDK_INT == 23) {
            back(false)
        }

        // Continue search on survival guide list
        click(id("survival_guide_search_list_btn"))

        isVisible(R.id.list)
        hasText(id("search"), "Eating fish")
    }
}
