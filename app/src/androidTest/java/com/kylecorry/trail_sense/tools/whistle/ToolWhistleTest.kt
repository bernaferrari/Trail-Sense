package com.kylecorry.trail_sense.tools.whistle

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isFalse
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isTrue
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test


class ToolWhistleTest : ToolTestBase(ToolIds.WHISTLE) {

    @Test
    fun verifyBasicFunctionality() {
        // Whistle button (needs to press and hold)
        click(desc(string(R.string.whistle_hold)), holdDuration = 200)

        // SOS button
        click(string(R.string.sos), exact = true)

        isTrue {
            TestUtils.isPlayingMusic()
        }

        click(string(R.string.sos), exact = true)

        isFalse {
            TestUtils.isPlayingMusic()
        }

        // Help (the emergency call)
        click(string(R.string.help), exact = true)

        isTrue {
            TestUtils.isPlayingMusic()
        }

        click(string(R.string.help), exact = true)

        isFalse {
            TestUtils.isPlayingMusic()
        }

        verifyQuickAction()
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(ToolIds.QUICK_ACTION_WHISTLE), holdDuration = 200)
        TestUtils.closeQuickActions()
    }
}
