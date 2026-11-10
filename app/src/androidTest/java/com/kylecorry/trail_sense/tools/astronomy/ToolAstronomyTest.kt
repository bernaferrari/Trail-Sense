package com.kylecorry.trail_sense.tools.astronomy

import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.views.desc
import com.kylecorry.trail_sense.test_utils.byResName
import java.util.regex.Pattern
import androidx.test.uiautomator.By
import org.junit.Assert.assertEquals
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.delay
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isNotVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.longClick
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.handleExactAlarmsDialog
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolAstronomyTest : ToolTestBase(Tools.ASTRONOMY, location) {

    @Test
    fun verifyBasicFunctionality() {
        // Verify the title
        hasText(R.id.astronomy_title) {
            val valid = listOf(
                string(R.string.until_sunset),
                string(R.string.until_sunrise)
            )
            valid.contains(it)
        }

        hasText(R.id.astronomy_title, Regex("([0-9]+h)? ?([0-9]+m)?"))

        // Verify that today is selected
        hasText(id("date"), string(R.string.today))

        // Verify the list of astronomy events is displayed
        hasText(id("astronomy_detail_title")) {
            it.startsWith(string(R.string.sun))
        }

        hasText(id("astronomy_detail_title", index = 1)) {
            it.startsWith(string(R.string.moon))
        }

        verifyQuickActions()

        // Verify the View in 3D button is visible and works
        if (Tools.isToolAvailable(TestUtils.context, Tools.AUGMENTED_REALITY)) {
            // Wait for the disclaimer to disappear: it is a long snackbar now (with a learn more action), not a toast
            not(waitForTime = 15000) {
                hasText(
                    string(R.string.sunset_alerts_background_location_disclaimer)
                )
            }
            scrollUntil { click(id("button_3d")) }

            // Verify the AR tool is open
            delay(500)
            clickOk()
            if (!Sensors.hasGyroscope(TestUtils.context)) {
                clickOk()
            }
            // Astronomy mode then asks what to locate (after the disclaimers here); cancel it, as upstream's test did
            click(string(android.R.string.cancel), exact = true)
            isVisible(desc(string(R.string.calibrate)))
        } else {
            not { isVisible(id("button_3d"), waitForTime = 0) }
        }
    }

    @Test
    fun verifyNightMode() {
        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_NIGHT_MODE))

        // TODO: Need to find a way to test this on staging builds
        if (AutomationLibrary.packageName == null) {
            // MainActivity is a ComponentActivity, so night mode is drawn by the Compose theme from the theme
            // preference instead of the activity's uiMode
            waitFor {
                assertEquals(UserPreferences.Theme.Night, UserPreferences(TestUtils.context).theme)
            }
        }

        TestUtils.openQuickActions()
        click(quickAction(Tools.QUICK_ACTION_NIGHT_MODE))
    }

    private fun verifyQuickActions() {
        // Verify the sunset alert quick action
        TestUtils.openQuickActions()
        click({ quickAction(Tools.QUICK_ACTION_SUNSET_ALERT) })

        handleExactAlarmsDialog()

        click({ quickAction(Tools.QUICK_ACTION_SUNSET_ALERT) })

        click({ quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT) })

        handleExactAlarmsDialog()

        click({ quickAction(Tools.QUICK_ACTION_SUNRISE_ALERT) })

        // TODO: Simulate time passing to verify the alerts are shown

        TestUtils.closeQuickActions()
    }

    @Test
    fun canMoveBetweenDates() {
        hasText(id("date_btn"), string(R.string.today))

        click(id("next_date"))
        hasText(id("date_btn"), string(R.string.tomorrow))

        click(id("prev_date"))
        click(id("prev_date"))
        hasText(id("date_btn"), string(R.string.yesterday))

        longClick(id("date_btn"))
        hasText(id("date_btn"), string(R.string.today))
    }

    @Test
    fun canFindTheNextFullMoonAndNewMoon() {
        goToDate(2025, 3, 1)

        findNext("Full Moon")
        // Full moon: March 14, 2025 06:55 UTC
        hasText(id("date_btn"), Regex("March 1[345], 2025"), waitForTime = SEARCH_TIMEOUT)

        findNext("New Moon")
        // New moon: March 29, 2025 10:58 UTC
        hasText(id("date_btn"), Regex("March (28|29|30), 2025"), waitForTime = SEARCH_TIMEOUT)
    }

    @Test
    fun timeSeekerFollowsTapsOnTheChart() {
        goToDate(2025, 6, 21)

        click(id("sunMoonChart"), xPercent = 0.5f, yPercent = 0.5f)
        val middle = waitFor { seekTime() }

        click(id("sunMoonChart"), xPercent = 0.9f, yPercent = 0.5f)
        waitFor {
            assertTrue(seekTime() != middle)
        }
    }

    /**
     * The time shown for the time seeker, which is the time label just above the seek bar.
     */
    private fun seekTime(): String {
        val seeker = view(byResName("time_seeker")).uiObject.visibleBounds
        return TestUtils.device.findObjects(By.text(Pattern.compile("\\d{1,2}:\\d{2}.*")))
            .filter { it.visibleBounds.bottom <= seeker.top }
            .maxBy { it.visibleBounds.bottom }
            .text
    }

    private fun goToDate(year: Int, month: Int, day: Int) {
        click(id("date_btn"))
        TestUtils.pickDate(year, month, day)
    }

    private fun findNext(event: String) {
        click(id("search_btn"))
        // Picking an event starts the search, there is no confirmation
        click(event, exact = true)
    }

    companion object {
        private val location = Coordinate(42.03, -71.97)
        private const val SEARCH_TIMEOUT = 30000L
    }
}
