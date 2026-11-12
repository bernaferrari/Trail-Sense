package com.kylecorry.trail_sense.tools.clouds

import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.views.clickableIn
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolCloudsTest : ToolTestBase(ToolIds.CLOUDS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(string(R.string.clouds))
        hasText(id("cloud_empty_text"), string(R.string.no_clouds))

        canScanCloudFromCamera()
        canScanCloudFromFile()
        canManuallyEnterCloud()
        verifyQuickAction()
    }

    private fun canScanCloudFromCamera() {
        click(id("add_btn"))

        click(string(R.string.camera))

        click(desc(TestUtils.getString(R.string.camera_capture)))  // The camera is a full screen dialog, so its tags are not exposed

        hasText(string(R.string.clouds))

        // Verify a cloud is selected (the top one)
        isChecked(id("cloud_selection_checkbox"))

        click(id("cloud_save_btn"))

        // Back on cloud log
        isVisible(id("add_btn"))
        click(clickableIn("cloud_list"))
        clickOk()
        isVisible(id("cloud_list"))

        // Delete all cloud results
        try {
            while (true) {
                click(id("cloud_item_menu"))
                click(string(R.string.delete))
                clickOk()
            }
        } catch (e: Throwable) {
            // Do nothing
        }

        hasText(id("cloud_empty_text"), string(R.string.no_clouds))
    }

    private fun canScanCloudFromFile() {
        click(id("add_btn"))
        click(string(R.string.file))

        // The file picker is opened
        waitFor {
            TestUtils.not { id("add_btn")() }
        }

        backUntil {
            hasText(string(R.string.clouds))
        }
    }

    private fun canManuallyEnterCloud() {
        click(id("add_btn"))

        click(string(R.string.manual))

        hasText(string(R.string.clouds))

        // Select the first cloud type
        click(id("cloud_selection_checkbox"))

        // Verify it is selected
        isChecked(id("cloud_selection_checkbox"))

        click(id("cloud_save_btn"))

        // Delete the cloud result
        click(id("cloud_item_menu"))
        click(string(R.string.delete))
        clickOk()

        hasText(id("cloud_empty_text"), string(R.string.no_clouds))
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(ToolIds.QUICK_ACTION_SCAN_CLOUD))

        click(R.id.capture_button)

        hasText(string(R.string.clouds))

        // Verify a cloud is selected (the top one)
        isChecked(id("cloud_selection_checkbox"))

        click(id("cloud_save_btn"))

        // Back on cloud log
        isVisible(id("add_btn"))
        isVisible(id("cloud_list"))
    }
}
