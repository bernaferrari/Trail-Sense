package com.kylecorry.trail_sense.tools.pedometer

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.clickListItemMenu
import com.kylecorry.trail_sense.test_utils.TestUtils.closeQuickActions
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.openQuickActions
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.DistanceAlerter
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test

class ToolPedometerTest : ToolTestBase(Tools.PEDOMETER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.PEDOMETER)) {
            return
        }

        hasText("0 ft")
        hasText("0", exact = true)
        hasText(string(R.string.steps))

        hasText("-")
        hasText(string(R.string.current_speed))

        hasText("-", index = 1)
        hasText(string(R.string.average_speed))

        hasText(string(R.string.off))

        click(id("play_btn"))
        hasText(string(R.string.on))
        hasNotification(StepCounterService.NOTIFICATION_ID, title = string(R.string.pedometer))

        click(id("play_btn"))
        doesNotHaveNotification(StepCounterService.NOTIFICATION_ID)
        hasText(string(R.string.off))

        click(id("reset_btn"))
        clickOk()

        hasText(Regex("since \\d+:\\d+ [AP]M"))

        click(id("pedometer_alert_btn"))
        input(R.id.amount, "0")
        clickOk()

        click(id("play_btn"))
        hasNotification(DistanceAlerter.NOTIFICATION_ID, title = string(R.string.distance_alert))

        click(id("play_btn"))

        verifyHistory()

        verifyQuickAction()
    }

    private fun verifyHistory() {
        scrollUntil(R.id.pedometer_scroll) {
            hasText(R.id.history_title, string(R.string.history))
        }

        hasText(R.id.hourly_steps_date, string(R.string.today))
        hasText(R.id.hourly_steps_total_steps, "0")
        hasText(R.id.hourly_steps_total_steps, string(R.string.steps))
        hasText(R.id.hourly_steps_total_distance, "0 ft")
        hasText(R.id.hourly_steps_total_distance, string(R.string.distance))

        click(R.id.prev_date)
        hasText(R.id.hourly_steps_date, string(R.string.yesterday))
        click(R.id.next_date)
        hasText(R.id.hourly_steps_date, string(R.string.today))

        scrollUntil(R.id.pedometer_scroll) {
            isVisible(R.id.hourly_steps_chart)
        }

        click(R.id.pedometer_sessions_btn)
        hasText(string(R.string.history))
        hasText("0 steps")
        clickListItemMenu("Delete")
        clickOk()
        not { hasText("0 steps") }
        clickOk()
    }

    private fun verifyQuickAction() {
        openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_PEDOMETER))
        hasNotification(StepCounterService.NOTIFICATION_ID, title = string(R.string.pedometer))
        click(quickAction(Tools.QUICK_ACTION_PEDOMETER))
        doesNotHaveNotification(StepCounterService.NOTIFICATION_ID)
        closeQuickActions()
    }

}
