package com.kylecorry.trail_sense.tools.whitenoise

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.notifications.hasTitle
import com.kylecorry.trail_sense.test_utils.notifications.notification
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoiseService
import org.junit.Test


class ToolWhiteNoiseTest : ToolTestBase(ToolIds.WHITE_NOISE) {

    @Test
    fun verifyBasicFunctionality() {
        // Turn on white noise
        click(desc(string(R.string.tool_white_noise_title)))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(WhiteNoiseService.NOTIFICATION_ID)
                    .hasTitle(R.string.tool_white_noise_title)
            }
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        // Turn it off
        click(desc(string(R.string.tool_white_noise_title)))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        }
        isFalse {
            TestUtils.isPlayingMusic()
        }

        // TODO: The UIAutomator can't enter text in the duration input
//        canSetSleepTimer()

        canChangeSleepSound()

        verifyQuickAction()
    }

    private fun canChangeSleepSound() {
        // The sounds are listed inline and a tap selects one, no dialog
        click("Pink noise", exact = true)
        click("Crickets", exact = true)
        hasText("Crickets", exact = true)
    }

    private fun canSetSleepTimer() {
        click(string(R.string.sleep_timer), exact = true)
        input(R.id.duration, "2")

        // Turn on white noise
        click(desc(string(R.string.tool_white_noise_title)))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(WhiteNoiseService.NOTIFICATION_ID)
                    .hasTitle(R.string.tool_white_noise_title)
            }
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        // Wait for the sleep timer to turn off the white noise
        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        }
        isFalse { TestUtils.isPlayingMusic() }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(ToolIds.QUICK_ACTION_WHITE_NOISE))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            waitFor {
                notification(WhiteNoiseService.NOTIFICATION_ID)
                    .hasTitle(R.string.tool_white_noise_title)
            }
        }

        isTrue {
            TestUtils.isPlayingMusic()
        }


        click(quickAction(ToolIds.QUICK_ACTION_WHITE_NOISE))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            not { notification(WhiteNoiseService.NOTIFICATION_ID) }
        }

        isFalse { TestUtils.isPlayingMusic() }

        TestUtils.closeQuickActions()
    }
}
