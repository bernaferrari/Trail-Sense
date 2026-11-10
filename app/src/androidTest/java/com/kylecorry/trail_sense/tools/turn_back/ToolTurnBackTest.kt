package com.kylecorry.trail_sense.tools.turn_back

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.pickTime
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolTurnBackTest : ToolTestBase(Tools.TURN_BACK) {

    @Test
    fun verifyBasicFunctionality() {
        // With no return time the screen asks for one
        hasText(id("instructions"), string(R.string.places_turn_back_idle_title))

        // Enter a time
        click(id("edittext"))
        pickTime(8, 0, false)

        handleExactAlarmsDialog()

        hasText(id("edittext"), "8:00 PM")
        hasText(
            id("instructions"),
            Regex("(?s).*(Turn around at \\d+:\\d+ (AM|PM)|${string(R.string.places_turn_back_turn_around_now)}).*")
        )
        scrollUntil { click(id("cancel_button")) }
        hasText(id("instructions"), string(R.string.places_turn_back_idle_title))

        // Return before dark
        click(id("sunset_button"))

        // Loading dialog is possible
        optional {
            hasText(string(R.string.loading))
            not(GPS_WAIT_FOR_TIMEOUT) { hasText(string(R.string.loading)) }
        }

        handleExactAlarmsDialog()
        hasText(
            id("edittext"),
            Regex("\\d+:\\d+ (AM|PM).*"),
            waitForTime = GPS_WAIT_FOR_TIMEOUT
        )
    }

}
