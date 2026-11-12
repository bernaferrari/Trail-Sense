package com.kylecorry.trail_sense.tools

import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.closeQuickActions
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolsTabTest : ToolTestBase(0L) {

    private val searchBox = { view(By.clazz("android.widget.EditText")) }

    @Test
    fun verifyBasicFunctionality() {
        canPinTools()
        canSortTools()
        canUseQuickActions()
        canUseBottomSheetQuickActions()
        canOpenToolUserGuide()
        canSearch()
        canOpenTool()
        canOpenSettings()
        canOpenToolSettings()
    }

    private fun canOpenSettings() {
        click(desc(string(R.string.settings)))
        hasText(string(R.string.general))
        back()
    }

    private fun canSortTools() {
        // The Compose tools tab always groups by category; choosing a sort (by name) is no longer offered there.
        hasText(string(R.string.tool_category_signaling))
        hasText(string(R.string.flashlight_title))
    }

    private fun canPinTools() {
        hasText(string(R.string.pinned))

        // Unpin the default pins, each from its long-press menu (the pinned tile comes first). The defaults
        // come from the preferences so the test follows the app's choice of starting pins.
        val pinned = UserPreferences(context).toolPinnedIds
            .filter { getAppService<ToolCatalog>().isToolAvailable(it) }
            .mapNotNull { Tools.getTool(context, it)?.name }
        pinned.forEach {
            longClick(it)
            click(string(R.string.unpin), exact = true)
        }

        not { hasText(string(R.string.pinned)) }

        // Pin Flashlight
        longClick(string(R.string.flashlight_title))
        click(string(R.string.pin), exact = true)

        hasText(string(R.string.pinned))
        hasText(string(R.string.flashlight_title), index = 1)

        // Unpin flashlight
        longClick(string(R.string.flashlight_title))
        click(string(R.string.unpin), exact = true)
    }

    private fun canUseQuickActions() {
        click(quickAction(ToolIds.QUICK_ACTION_WHISTLE))
    }

    private fun canUseBottomSheetQuickActions() {
        openQuickActions()
        click(quickAction(ToolIds.QUICK_ACTION_WHISTLE))
        closeQuickActions()
    }

    private fun canOpenToolUserGuide() {
        longClick(string(R.string.flashlight_title))
        click(string(R.string.tool_user_guide_title))
        hasText("SOS")
        back(false)
    }

    private fun canOpenToolSettings() {
        longClick(string(R.string.flashlight_title))
        click(string(R.string.settings))
        hasText(string(R.string.pref_flashlight_control_screen_with_volume_title))
        back()
        isVisible(desc(string(R.string.settings)))
    }

    private fun canSearch() {
        input(searchBox, "Sett")
        hasText(string(R.string.settings))
        input(searchBox, "")
        hasText(string(R.string.flashlight_title))
    }

    private fun canOpenTool() {
        click(string(R.string.flashlight_title))
        isVisible(id("screen_flashlight_btn"))
        back()
        isVisible(desc(string(R.string.settings)))
    }

}
