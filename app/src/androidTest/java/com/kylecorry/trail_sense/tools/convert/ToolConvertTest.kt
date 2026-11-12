package com.kylecorry.trail_sense.tools.convert

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.any
import com.kylecorry.trail_sense.test_utils.views.TestView
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.By
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import org.junit.Test

class ToolConvertTest : ToolTestBase(ToolIds.CONVERT) {
    @Test
    fun verifyBasicFunctionality() {
        canConvertCoordinates()
        canConvertDistance()
        canConvertTemperature()
        canConvertVolume()
        canConvertWeight()
        canConvertTime()
        verifyQuickAction()
    }

    private fun canConvertCoordinates() {
        // The converter opens on distance, so pick the coordinates category first. It is the first chip, which may
        // be scrolled out of the category row.
        scrollUntil({ categoryRow() }, direction = Direction.LEFT) {
            click(string(R.string.coordinates_tab), exact = true)
        }
        inputAmount("42, -72")
        val format = FormatService.getInstance(TestUtils.context)
        // The converter remembers the output format, so the second pass (from the quick action) already shows USNG
        any(
            {
                pickUnit(
                    format.formatCoordinateType(BuiltInCoordinateFormat.entries.first()),
                    string(R.string.coordinate_format_usng)
                )
            },
            { hasText(string(R.string.coordinate_format_usng), exact = true, waitForTime = 0) }
        )
        hasText("19T BG 51535 54131", exact = true)
    }

    private fun canConvertDistance() {
        click(string(R.string.distance))
        hasText(string(R.string.unit_meters))
        inputAmount("1")

        pickUnit(string(R.string.unit_meters), string(R.string.unit_meters))
        pickUnit(string(R.string.unit_feet), string(R.string.unit_feet))

        swapBackIfRemembered("0.3048")
        hasText("3.2808", exact = true)

        // Swap
        click(desc(string(R.string.convert_swap_units)))
        hasText("0.3048", exact = true)
    }

    private fun canConvertTemperature() {
        click(string(R.string.temperature))
        hasText(string(R.string.celsius))
        inputAmount("0")

        pickUnit(string(R.string.celsius), string(R.string.celsius))
        pickUnit(string(R.string.fahrenheit), string(R.string.fahrenheit))

        swapBackIfRemembered("-17.7778")
        hasText("32", exact = true)

        // Swap
        click(desc(string(R.string.convert_swap_units)))
        hasText("-17.7778", exact = true)
    }

    private fun canConvertVolume() {
        click(string(R.string.volume))
        hasText(string(R.string.liters))
        inputAmount("1")

        pickUnit(string(R.string.liters), string(R.string.liters))
        pickUnit(string(R.string.us_gallons), string(R.string.us_gallons))

        hasText("0.2642", exact = true)

        // Swap
        click(desc(string(R.string.convert_swap_units)))
        hasText("3.7854", exact = true)
    }

    private fun canConvertWeight() {
        click(string(R.string.weight))
        hasText(string(R.string.kilograms))
        inputAmount("1")

        pickUnit(string(R.string.kilograms), string(R.string.kilograms))
        pickUnit(string(R.string.pounds), string(R.string.pounds))

        hasText("2.2046", exact = true)

        // Swap
        click(desc(string(R.string.convert_swap_units)))
        hasText("0.4536", exact = true)
    }

    private fun canConvertTime() {
        click(string(R.string.time))
        hasText(string(R.string.minutes))
        inputAmount("60")

        pickUnit(string(R.string.minutes), string(R.string.minutes))
        pickUnit(string(R.string.hours), string(R.string.hours))

        hasText("1", exact = true)

        // Swap
        click(desc(string(R.string.convert_swap_units)))
        hasText("3600", exact = true)
    }

    /**
     * The converter remembers its units, so the second pass (from the quick action) starts with the
     * units swapped by the first pass's swap check, showing [swappedResult]. Swap them back.
     */
    private fun swapBackIfRemembered(swappedResult: String) {
        optional {
            hasText(swappedResult, exact = true, waitForTime = 1000)
            click(desc(string(R.string.convert_swap_units)))
        }
    }

    /**
     * Types into the converter's input row, the only text field on the page.
     */
    private fun inputAmount(text: String) {
        input({ view(By.clazz("android.widget.EditText")) }, text)
    }

    /**
     * Opens the units of the row whose pill reads [current] and picks [unit] from the grid that replaces the keyboard.
     * The grid is under the rows, so when the row already shows [unit] the grid's copy is the second match.
     */
    private fun pickUnit(current: String, unit: String) {
        click(current, exact = true)
        click(unit, exact = true, index = if (current == unit) 1 else 0)
    }

    /** The horizontally scrolling row of category chips: the short scrollable at the top of the converter. */
    private fun categoryRow(): TestView {
        val row = TestUtils.device.findObjects(By.scrollable(true).pkg(TestUtils.context.packageName))
            .minByOrNull { it.visibleBounds.height() }
            ?: throw AssertionError("Category row not found")
        return TestView(row)
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(ToolIds.QUICK_ACTION_CONVERT))

        canConvertCoordinates()
        canConvertDistance()
        canConvertTemperature()

        // Tapping a quick action closes the quick actions. The converter sheet's first back can only hide the
        // keyboard, so press back until the sheet is gone.
        backUntil {
            not { isVisible(desc(string(R.string.convert_swap_units)), waitForTime = 0) }
        }
    }

}
