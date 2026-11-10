package com.kylecorry.trail_sense.tools.field_guide

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.pickDate
import com.kylecorry.trail_sense.test_utils.TestUtils.pickTime
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolFieldGuideTest : ToolTestBase(Tools.FIELD_GUIDE) {

    @Test
    fun verifyBasicFunctionality() {
        // Disclaimer
        clickOk()

        hasCategories()
        canOpenPage()
        canCreatePage()
        canRecordSightingWithQuickAction()
        canManageMultipleImages()
        canEditPage()
        canDeletePage()
        canSearch()
    }

    private fun canSearch() {
        back(false)
        input(id("search"), "Rabbit")
        hasText("Rabbit")

        input(id("search"), "Insect")
        hasText("Ant")

        back(false)
        hasText("Plant")
    }

    private fun hasCategories() {
        // Shows categories
        for (category in listOf("Plant", "Other")) {
            scrollUntil {
                hasText(category)
            }
        }

        // Open a section
        scrollToStart(R.id.list)
        click("Animal")
        hasText("Ant")
        hasText("Clam")
    }

    private fun canDeletePage() {
        click(id("field_guide_page_menu"))
        click("Delete")
        clickOk()
        not { hasText("A Test 3") }
    }

    private fun canManageMultipleImages() {
        clickListItemMenu("Edit")

        // A single image was added when the page was created
        scrollUntil(R.id.scroll_view) { hasText(R.id.photo_position, "Image 1/1") }

        // The slot after the last image is used to add another one
        click(R.id.next_photo_button)
        scrollUntil(R.id.scroll_view) { click("Take photo") }
        waitFor {
            // The click can land before the list settles, so repeat it until the camera is open
            try {
                isVisible(R.id.capture_button, waitForTime = 1)
            } catch (e: Throwable) {
                click("Take photo", waitForTime = 1)
                throw e
            }
        }
        click(R.id.capture_button)
        hasText(R.id.photo_position, "Image 2/2")

        // The arrows page through the images
        click(R.id.previous_photo_button)
        hasText(R.id.photo_position, "Image 1/2")
        click(R.id.next_photo_button)
        hasText(R.id.photo_position, "Image 2/2")

        // Setting an image as the default moves it to the first slot
        click(id("photo_upload_menu_button"))
        click("Set as default")
        hasText(R.id.photo_position, "Image 1/2")

        click(toolbarButton(Side.Right))

        // The page displays all the images
        click("A Test")
        hasText(R.id.photo_position, "Image 1/2")
        click(R.id.next_photo_button)
        hasText(R.id.photo_position, "Image 2/2")
        back()

        // An image can be deleted
        clickListItemMenu("Edit")
        scrollUntil(R.id.scroll_view) { hasText(R.id.photo_position, "Image 1/2") }
        click(id("photo_upload_menu_button"))
        click("Delete")
        clickOk()
        hasText(R.id.photo_position, "Image 1/1")

        click(toolbarButton(Side.Right))
    }

    private fun canEditPage() {
        click(id("field_guide_page_menu"))
        click("Edit")
        hasText(id("name"), "A Test")
        input(id("name"), "A Test 2")
        click(id("create_field_guide_page_save_btn"))

        // Verify the changes
        hasText("A Test 2")

        // Open it an edit
        click("A Test 2")
        click(id("field_guide_page_edit_btn"))
        hasText(id("name"), "A Test 2")
        input(id("name"), "A Test 3")
        click(id("create_field_guide_page_save_btn"))

        // Verify the changes
        hasText("A Test 3")

        back()
    }

    private fun canOpenPage() {
        click("Ant")
        hasText("Ant")
        hasText("A small insect")
        canLogSightings()
        scrollUntil {
            hasText("Grassland")
        }
        back()
    }

    private fun canRecordSightingWithQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(QUICK_ACTION_RECORD_SIGHTING))
        input(R.id.search, "A Test")
        click("A Test", exact = true, index = 1)

        hasText("Created", waitForTime = GPS_WAIT_FOR_TIMEOUT)
        click("View")
        input("Notes", "Quick action sighting")
        click(toolbarButton(Side.Right))

        // The quick action deep link returns to the sightings list after saving.
        hasText("Quick action sighting")
        back()
        hasText("Sightings (1)")
        back()
    }

    private fun canCreatePage() {
        click(id("add_btn"))
        input(id("name"), "A Test")
        click(id("tag_classifications"))
        click("Bird")
        clickOk()
        hasText(id("tag_classifications"), "Animal, Bird")
        scrollUntil { input(id("notes"), "Notes") }
        scrollToEnd(id("scroll_view"))

        click(id("tag_locations"))
        click("North America")
        clickOk()
        hasText(id("tag_locations"), "North America")

        click(id("tag_habitats"))
        click("Forest")
        clickOk()
        hasText(id("tag_habitats"), "Forest")

        click(id("tag_human_interactions"))
        click("Edible")
        click("Inedible")
        clickOk()
        hasText(id("tag_human_interactions"), "Edible, Inedible")

        click(id("tag_activity_patterns"))
        click("Diurnal (day)")
        clickOk()
        hasText(id("tag_activity_patterns"), "Diurnal (day)")

        scrollToStart(id("scroll_view"))
        click("Take photo")
        click(desc(TestUtils.getString(R.string.camera_capture)))  // The camera is a full screen dialog, so its tags are not exposed

        isVisible(id("image"))

        click(id("create_field_guide_page_save_btn"))

        // Verify it shows up in the list
        hasText("A Test")
        click("A Test")

        // Verify the details
        hasText("A Test")
        hasText("Notes")

        for (tag in listOf("Bird", "Diurnal (day)")) {
            scrollUntil {
                hasText(tag)
            }
        }

        back()
    }

    private fun canLogSightings() {
        // The page shows a sightings row with its count, which opens the list
        hasText(id("field_guide_page_sightings"), "0 sightings")
        click(id("field_guide_page_sightings"))
        hasText(string(R.string.no_sightings))
        click(id("create_btn"))
        click(id("time"))
        pickDate(2025, 1, 1)
        pickTime(6, 0, true)
        hasText("January 1, 2025 6:00 AM")
        input(id("location"), "42, 72")
        input(id("elevation"), "100")
        click(id("harvested"))
        click(id("show_on_map"))
        // The notes field's label sits above it, outside the field
        input(id("notes"), "Test")
        click(id("field_guide_sighting_save_btn"))

        // Sightings list
        hasText("Sightings")
        hasText("January 1, 2025 6:00 AM")
        hasText("Test")

        // Edit by clicking
        click("January 1, 2025 6:00 AM")
        hasText("January 1, 2025 6:00 AM")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        isChecked(id("harvested"))
        isNotChecked(id("show_on_map"))
        click(id("show_on_map"))
        input("Test", "Test 2")
        click(id("field_guide_sighting_save_btn"))

        // Sightings list is updated
        hasText("Test 2")

        // Edit via menu
        click(id("field_guide_sighting_menu"))
        click("Edit")
        hasText("January 1, 2025 6:00 AM")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        isChecked(id("harvested"))
        isChecked(id("show_on_map"))
        input("Test 2", "Test 3")
        click(id("field_guide_sighting_save_btn"))

        // Sightings list is updated
        hasText("Test 3")

        // Navigate
        click(id("field_guide_sighting_menu"))
        click("Navigate")
        hasText(Regex("\\s*\\d+°\\s+[NSEW]+"))
        hasText("Ant")
        back()

        // Create beacon
        click(id("field_guide_sighting_menu"))
        click(string(R.string.create_beacon))
        hasText("Ant")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        back()
        click("Leave")

        // Count on field guide page: the sightings row says how many there are, and opens the list
        backUntil {
            hasText(id("field_guide_page_sightings"), "1 sighting")
        }
        click(id("field_guide_page_sightings"))

        // Delete from the list
        click(id("field_guide_sighting_menu"))
        click("Delete")
        clickOk()

        hasText("No sightings")

        // Create another sighting to delete from the edit page
        click(R.id.create_btn)
        input("Notes", "Delete from edit page")
        click(toolbarButton(Side.Right))
        hasText("Delete from edit page")

        // Delete from the edit page
        click("Delete from edit page")
        click(toolbarButton(Side.Left))
        click("Delete")
        clickOk()

        hasText("No sightings")
        back()
        hasText("Sightings (0)")
    }
}

/**
 * Upstream's "Record Sighting" quick action (#3860). The Compose app no longer registers it, so this
 * step fails until the quick action is restored in FieldGuideToolRegistration and Tools.
 */
private const val QUICK_ACTION_RECORD_SIGHTING = 30
