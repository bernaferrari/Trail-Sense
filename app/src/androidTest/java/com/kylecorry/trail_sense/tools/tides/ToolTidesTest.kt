package com.kylecorry.trail_sense.tools.tides

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToEnd
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolTidesTest : ToolTestBase(Tools.TIDES) {

    @Test
    fun verifyBasicFunctionality() {
        // Wait for the disclaimer
        clickOk()

        // Wait for the nearby tides to load
        hasText(string(R.string.navigation_nearby_category), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        click(id("tide_table_btn"))

        // Tide list
        hasText(id("tide_list_title"), string(R.string.tides))
        hasText(string(R.string.navigation_nearby_category))
        hasText(string(R.string.tide_model_auto))

        canCreateTide()
        canViewTide()
        canOpenTideList()
        canEditTide()
    }

    private fun canEditTide() {
        click(id("tide_table_menu", index = 1))
        click(string(R.string.edit))

        hasText(string(R.string.tide_table))

        // Verify the fields are set
        hasText(R.id.tide_name) { it.startsWith("Tide 1") }

        // Select lunitidal interval
        click(R.id.estimate_algorithm_spinner)
        click(string(R.string.lunitidal_interval_auto))
        clickOk()

        // Verify the fields are set
        hasText(R.id.estimate_algorithm_spinner, string(R.string.lunitidal_interval_auto)
        )

        // TODO: Manual lunitidal interval

        // Save
        click(id("create_tide_save_btn"))

        hasText(id("tide_list_title"), string(R.string.tides))
        hasText(id("tide_table_title", index = 1), "Tide 1")

        // Open the tide
        click(id("tide_table_title", index = 1))

        // Verify it is now using the lunitidal interval (just check times are shown)
        hasText(string(R.string.high_tide))
    }

    private fun canCreateTide() {
        // Click the add button
        click(id("add_btn"))

        // Enter the tide details
        hasText(string(R.string.tide_table))
        input(R.id.tide_name, "Tide 1")
        isChecked(R.id.tide_frequency_semidiurnal)
        input(R.id.utm, "42, -72")

        click(R.id.estimate_algorithm_spinner)
        isChecked(string(R.string.tide_clock))
        hasText(string(R.string.lunitidal_interval_auto))
        clickOk()

        scrollToEnd(id("scroll_view"))

        hasText(id("tide_type"), string(R.string.high_tide_letter))
        click(id("tide_type"))
        hasText(id("tide_type"), string(R.string.low_tide_letter))
        click(id("tide_type"))
        hasText(id("tide_type"), string(R.string.high_tide_letter))

        click(id("tide_time", index = 1))
        clickOk()
        clickOk()

        // TODO: Verify the time is set
        hasText(id("tide_time", index = 1)) { it.isNotBlank() }

        click(id("tide_height", index = 1))
        input(string(R.string.distance), "1.0")
        clickOk()

        hasText(id("tide_height", index = 1), "1.00 ft")

        click(R.id.add_tide_entry)

        hasText(id("tide_type", index = 1), string(R.string.high_tide_letter))
        click(id("delete", index = 1))

        not {
            hasText(id("tide_type", index = 1), string(R.string.high_tide_letter))
        }

        click(id("create_tide_save_btn"))

        hasText(id("tide_list_title"), string(R.string.tides))
        hasText(id("tide_table_title", index = 1), "Tide 1")
        hasText(id("tide_table_description", index = 1), "1 tide")
    }

    private fun canViewTide() {
        click(id("tide_table_title", index = 1))
        hasText(string(R.string.high_tide))
        hasText("Tide 1")

        // Verify that today is selected
        hasText(R.id.tide_list_date, string(R.string.today))

        // Verify at least one high and low tide is shown
        var isHighFirst = false
        hasText(com.kylecorry.andromeda.views.R.id.title) {
            isHighFirst = it == string(R.string.high_tide)
            it == string(R.string.high_tide) || it == string(R.string.low_tide)
        }

        hasText(
            com.kylecorry.andromeda.views.R.id.title, string(
                if (isHighFirst) {
                    R.string.low_tide
                } else {
                    R.string.high_tide
                }
            ), index = 1
        )

        // TODO: Verify the times are correct
        // TODO: Verify the chart
    }

    private fun canOpenTideList() {
        click(id("tide_table_btn"))

        hasText(id("tide_list_title"), string(R.string.tides))
        hasText(id("tide_table_title", index = 1), "Tide 1")
    }
}
