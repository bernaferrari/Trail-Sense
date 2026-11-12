package com.kylecorry.trail_sense.tools.lightning

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import org.junit.Test

class ToolLightningStrikeDistanceTest : ToolTestBase(ToolIds.LIGHTNING_STRIKE_DISTANCE) {

    @Test
    fun verifyBasicFunctionality() {
        // Ready to record lightning
        hasText(id("lightning_button"), string(R.string.lightning))
        hasText(id("lightning_verdict"), string(R.string.lightning_hint_flash))

        // Lightning: now counting until the thunder
        click(id("lightning_button"))
        hasText(id("lightning_button"), string(R.string.thunder))
        hasText(id("lightning_verdict"), string(R.string.lightning_hint_thunder))

        // Thunder right away: the strike is close enough to be dangerous
        click(id("lightning_button"))
        hasText(id("lightning_title"), Regex("[0-9.]+"))
        hasText(id("lightning_verdict"), string(R.string.lightning_verdict_danger))
        hasText(id("lightning_button"), string(R.string.lightning_again))

        // Start over, and the last strike stays as the one to compare against
        click(id("lightning_button"))
        hasText(id("lightning_button"), string(R.string.lightning))
        click(id("lightning_button"))
        click(id("lightning_button"))
        hasText(id("lightning_title"), Regex("[0-9.]+"))
        hasText(id("lightning_verdict"), string(R.string.lightning_verdict_danger))
    }
}
