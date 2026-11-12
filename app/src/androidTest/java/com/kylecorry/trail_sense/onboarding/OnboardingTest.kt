package com.kylecorry.trail_sense.onboarding

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.RetryTestRule
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.back
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** First run lives in MainActivity as Navigation 3 pages, in place of the app until it is finished. */
class OnboardingTest {

    @get:Rule
    val retryRule = RetryTestRule(maxRetryCount = 3)

    @get:Rule
    val grantPermissionRule = TestUtils.allPermissionsGranted()

    private val prefs by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        TestUtils.setWaitForIdleTimeout()
        TestUtils.setupApplication()
        prefs.putBoolean(PreferenceKeys.onboardingCompleted, false)
        prefs.putBoolean(PreferenceKeys.mainDisclaimerShownKey, false)
        prefs.putString(PreferenceKeys.distanceUnits, "feet_miles")
    }

    @After
    fun tearDown() {
        scenario?.close()
        // Every other test expects a finished first run
        TestUtils.setupDefaultPreferences()
    }

    @Test
    fun firstRunShowsOnboardingThenTheApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // The unit system page
        hasText(string(R.string.onboarding_setup_intro))
        click(string(R.string.metric), exact = true)
        click(string(R.string.next), exact = true)

        // The disclaimer; Back returns to the unit page without finishing first run
        hasText(string(R.string.app_disclaimer_message_title))
        back()
        hasText(string(R.string.onboarding_setup_intro))
        assertNotEquals(true, prefs.getBoolean(PreferenceKeys.onboardingCompleted))

        click(string(R.string.next), exact = true)
        click(string(R.string.i_agree), exact = true)
        // Shown only when the device has no compass
        optional {
            click(string(R.string.next), exact = true, waitForTime = 1000)
        }

        // The app itself, with first run saved
        hasText(string(R.string.tools), exact = true)
        assertEquals(true, prefs.getBoolean(PreferenceKeys.onboardingCompleted))
        assertEquals(true, prefs.getBoolean(PreferenceKeys.mainDisclaimerShownKey))
        assertEquals("meters", prefs.getString(PreferenceKeys.distanceUnits))

        // It stays finished across a recreation
        scenario?.recreate()
        hasText(string(R.string.tools), exact = true)
        TestUtils.not { hasText(string(R.string.onboarding_setup_intro), waitForTime = 1000) }
    }

    @Test
    fun backOnTheFirstPageLeavesTheApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        hasText(string(R.string.onboarding_setup_intro))

        back()

        waitFor {
            assertNotEquals(Lifecycle.State.RESUMED, scenario?.state)
        }
        assertNotEquals(true, prefs.getBoolean(PreferenceKeys.onboardingCompleted))
    }
}
