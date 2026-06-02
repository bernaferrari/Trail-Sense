package com.kylecorry.trail_sense.tools.battery

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolBatteryTest : ToolTestBase(Tools.BATTERY) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(Regex("\\d+%"))
        hasText(Regex("(Charging|Discharging).*"))
        hasText("Battery log")
        hasText("Updates every 1h - Low battery usage")

        isNotChecked(id("low_power_mode_switch"))
        click(id("low_power_mode_switch"))
        isChecked(id("low_power_mode_switch"))

        // Stop the battery log
        click(id("battery_service_stop_btn"))
        not { hasText("Battery log") }

        scrollUntil {
            click("Airplane mode")
        }
        optional {
            hasText("Network")
        }
        backUntil { isVisible(id("battery_title")) }

        hasListItems(
            "Airplane mode",
            "Wi-Fi",
            "Bluetooth",
            "NFC",
            "Location",
            "Battery saver",
            "Data saver",
            "Adaptive brightness",
            "Screen timeout",
            "Dark theme",
            "Restrict app background activity",
            "Other tips"
        )

        // Open battery settings
        click(id("battery_settings_btn"))
        hasText("Battery")
    }

    private fun hasListItems(vararg items: String) {
        for (item in items) {
            scrollUntil {
                hasText(item)
            }
        }
    }
}
