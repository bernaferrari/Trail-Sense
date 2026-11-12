package com.kylecorry.trail_sense.tools.diagnostics

import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolDiagnosticsTest : ToolTestBase(ToolIds.DIAGNOSTICS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(string(R.string.diagnostics))

        // Battery usage always starts restricted, so just check for that
        hasText(string(R.string.battery_usage_restricted), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        click(string(R.string.battery_usage_restricted))
        click(string(android.R.string.cancel))

        // Sensor details is the first row in the tools section
        click(id("diagnostics_title"))
        hasText(string(R.string.sensors))
    }

}
