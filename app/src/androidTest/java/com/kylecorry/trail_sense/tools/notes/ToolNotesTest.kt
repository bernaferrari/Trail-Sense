package com.kylecorry.trail_sense.tools.notes

import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolNotesTest : ToolTestBase(Tools.NOTES) {
    @Test
    fun verifyBasicFunctionality() {
        hasText(string(R.string.tool_notes_title))
        hasText(id("notes_empty_text"), string(R.string.notes_empty_text))

        canCreateNote()
        canCreateQRCode()
        canEditNote()
        canDeleteNote()
        verifyQuickAction()
    }

    private fun canEditNote() {
        click("Test note", exact = true)
        hasText(id("title_edit"), "Test note")
        hasText(id("content_edit"), "This is a test note")

        input(id("title_edit"), "Test note 2")
        input(id("content_edit"), "This is a test note 2")

        click(id("note_create_btn"))

        hasText("Test note 2", exact = true)
        hasText("This is a test note 2", exact = true)
    }

    private fun canDeleteNote() {
        clickListItemMenu(string(R.string.delete))
        clickOk()

        hasText(id("notes_empty_text"), string(R.string.notes_empty_text))
    }

    private fun canCreateQRCode() {
        clickListItemMenu(string(R.string.qr_code))
        hasText("Test note", exact = true)
        back(false)
    }

    private fun canCreateNote() {
        click(R.id.add_btn)

        input(id("title_edit"), "Test note")
        input(id("content_edit"), "This is a test note")

        click(id("note_create_btn"))

        hasText("Test note", exact = true)
        hasText("This is a test note", exact = true)
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_CREATE_NOTE))

        input(id("title_edit"), "Quick action note")
        input(id("content_edit"), "This is a quick action note")

        click(id("note_create_btn"))

        hasText("Quick action note", exact = true)
        hasText("This is a quick action note", exact = true)
    }
}
