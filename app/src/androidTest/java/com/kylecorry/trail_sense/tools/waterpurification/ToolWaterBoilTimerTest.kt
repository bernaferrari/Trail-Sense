package com.kylecorry.trail_sense.tools.waterpurification

import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimerService
import org.junit.Test

class ToolWaterBoilTimerTest : ToolTestBase(ToolIds.WATER_BOIL_TIMER) {

    private val dial = id("water_boil_dial")

    @Test
    fun verifyBasicFunctionality() {
        // Auto
        hasText(string(R.string.auto), exact = true)
        hasText(dial, Regex("[13]:00"), waitForTime = GPS_WAIT_FOR_TIMEOUT)

        // Select 3 minutes
        click("3m", exact = true)
        hasText(dial, Regex("3:00"))

        // Select 1 minute
        click("1m", exact = true)
        hasText(dial, Regex("1:00"))

        // Start the timer
        click(string(R.string.start), exact = true)

        // Verify it is started
        hasText(string(android.R.string.cancel), exact = true)
        hasText(dial, Regex("1:00|0:[0-5]\\d"))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            notification(WaterPurificationTimerService.NOTIFICATION_ID).hasTitle(R.string.water_boil_timer_title)
        }

        // TODO: Wait for the timer to finish and verify the finished state (simulate time passing)

        // Cancel the timer
        click(string(android.R.string.cancel), exact = true)

        // Verify it is stopped
        hasText(string(R.string.start), exact = true)
        hasText(dial, Regex("1:00"))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WaterPurificationTimerService.NOTIFICATION_ID) }
        }
    }
}
