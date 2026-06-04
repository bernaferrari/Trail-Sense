package com.kylecorry.trail_sense.tools.field_guide

import com.kylecorry.trail_sense.R
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
import com.kylecorry.trail_sense.test_utils.TestUtils.pickDate
import com.kylecorry.trail_sense.test_utils.TestUtils.pickTime
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
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
        canEditPage()
        canDeletePage()
        canSearch()
        canHideBuiltInPage()
    }

    private fun canHideBuiltInPage() {
        click("Animal")
        hasText("Ant")
        clickListItemMenu("Hide")
        clickOk()
        not { hasText("Ant") }
    }

    private fun canSearch() {
        back(false)
        input(R.id.search, "Rabbit")
        hasText("Rabbit")

        input(R.id.search, "Insect")
        hasText("Ant")

        back(false)
        hasText("Plant")
    }

    private fun hasCategories() {
        // Shows categories
        val categories = listOf(
            "Plant",
            "Fungus",
            "Animal",
            "Mammal",
            "Bird",
            "Reptile",
            "Amphibian",
            "Fish",
            "Invertebrate",
            "Rock",
            "Other"
        )

        for (category in categories) {
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

    private fun canEditPage() {
        click(id("field_guide_page_menu"))
        click("Edit")
        hasText(R.id.name, "A Test")
        input(R.id.name, "A Test 2")
        click(R.id.create_field_guide_page_save_btn)

        // Verify the changes
        hasText("A Test 2")

        // Open it an edit
        click("A Test 2")
        click(id("field_guide_page_edit_btn"))
        hasText(R.id.name, "A Test 2")
        input(R.id.name, "A Test 3")
        click(R.id.create_field_guide_page_save_btn)

        // Verify the changes
        hasText("A Test 3")

        back()
    }

    private fun canOpenPage() {
        click("Ant")
        hasText("Ant")
        hasText("A small insect")
        canLogSightings()
        val tags = listOf(
            "North America",
            "Insect",
            "Grassland",
        )
        for (tag in tags) {
            scrollUntil {
                hasText(tag)
            }
        }
        back()
    }

    private fun canRecordSightingWithQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_RECORD_SIGHTING))
        input(R.id.search, "A Test")
        click("A Test")

        hasText("Created")
        click("View")
        input("Notes", "Quick action sighting")
        click(toolbarButton(R.id.title, Side.Right))

        // The quick action deep link returns to the sightings list after saving.
        hasText("Quick action sighting")
        back()
        hasText("Sightings (1)")
        back()
    }

    private fun canCreatePage() {
        click(id("add_btn"))
        input(R.id.name, "A Test")
        click(R.id.tag_classifications)
        click("Bird")
        clickOk()
        hasText(R.id.tag_classifications, "Animal, Bird")
        input(R.id.notes, "Notes")
        scrollToEnd(R.id.scroll_view)

        click(R.id.tag_locations)
        click("North America")
        clickOk()
        hasText(R.id.tag_locations, "North America")

        click(R.id.tag_habitats)
        click("Forest")
        clickOk()
        hasText(R.id.tag_habitats, "Forest")

        click(R.id.tag_human_interactions)
        click("Edible")
        click("Inedible")
        clickOk()
        hasText(R.id.tag_human_interactions, "Edible, Inedible")

        click(R.id.tag_activity_patterns)
        click("Diurnal (day)")
        clickOk()
        hasText(R.id.tag_activity_patterns, "Diurnal (day)")

        scrollToStart(R.id.scroll_view)
        click("Take photo")
        click(R.id.capture_button)

        isVisible(R.id.image)

        click(R.id.create_field_guide_page_save_btn)

        // Verify it shows up in the list
        hasText("A Test")
        click("A Test")

        // Verify the details
        hasText("A Test")
        hasText("Notes")

        val tags = listOf(
            "Animal",
            "Bird",
            "North America",
            "Forest",
            "Edible",
            "Inedible",
            "Diurnal (day)"
        )

        for (tag in tags) {
            scrollUntil {
                hasText(tag)
            }
        }

        back()
    }

    private fun canLogSightings() {
        click("Sightings (0)")
        hasText("No sightings")
        click(id("create_btn"))
        click(R.id.time)
        pickDate(2025, 1, 1)
        pickTime(6, 0, true)
        hasText("January 1, 2025 6:00 AM")
        input(R.id.location, "42, 72")
        input(R.id.elevation, "100")
        click("Harvested")
        click("Show on map")
        input("Notes", "Test")
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
        isChecked("Harvested")
        isNotChecked("Show on map")
        click("Show on map")
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
        isChecked("Harvested")
        isChecked("Show on map")
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
        click("Create beacon")
        hasText("Ant")
        hasText("42.000000°,  72.000000°")
        hasText("100")
        back()
        click("Leave")

        // Count on field guide page
        backUntil {
            click("Sightings (1)")
        }

        // Delete
        click(id("field_guide_sighting_menu"))
        click("Delete")
        clickOk()

        hasText("No sightings")
        back()
        hasText("Sightings (0)")
    }
}
