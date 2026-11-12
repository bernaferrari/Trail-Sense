package com.kylecorry.trail_sense.tools.weather

import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.any
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.test_utils.AutomationLibrary
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.doesNotHaveNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasNotification
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isVisible
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.optional
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollToStart
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestData
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorService
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.Duration

class ToolWeatherTest : ToolTestBase(ToolIds.WEATHER) {

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, ToolIds.WEATHER)) {
            return
        }

        // Historic temperature disclaimer
        clickOk()

        not(waitForTime = AutomationLibrary.GPS_WAIT_FOR_TIMEOUT) {
            hasText("Updating weather")
        }

        // Weather prediction
        // The forecast is the headline card at the top (it has no tag)
        any(
            { hasText(string(R.string.weather_no_change), exact = true, waitForTime = 0) },
            { hasText(string(R.string.weather_overcast), exact = true, waitForTime = 0) },
            { hasText(string(R.string.weather_clear), exact = true, waitForTime = 0) },
            // Before the monitor has a few readings, the headline asks to start it
            { hasText(string(R.string.weather_empty_title), exact = true, waitForTime = 0) }
        )

        // Wait for a pressure reading
        waitFor(15000) {
            // The readings may still be loading while scrolling
            scrollToStart()
            scrollUntil { hasText(string(R.string.pressure)) }
        }
        // The value and its unit are drawn separately
        hasText(Regex("\\d+(\\.\\d+)?"))
        hasText("in", exact = true)

        // Historic temperature
        optional {
            hasText(string(R.string.temperature))
            hasText(string(R.string.historic_temperature_years, 30))
            hasText(Regex("\\d+(\\.\\d+)? °F"))
        }

        // High / low
        optional {
            hasText(string(R.string.temperature_high_low))
            hasText(Regex("\\d+(\\.\\d+)? °F / \\d+(\\.\\d+)? °F"))
        }

        // Last reading
        scrollUntil { hasText(string(R.string.last_updated)) }
        scrollToStart()

        // Pressure chart
        isVisible(id("chart"))

        // High / low chart
        optional {
            click(string(R.string.temperature_high_low))
            hasText(string(R.string.next_24_hours))
            isVisible(R.id.chart)
            clickOk()
        }

        canUseWeatherMonitor()
        verifyQuickAction()
    }

    private fun canUseWeatherMonitor() {
        // Without a forecast yet, the empty state's button starts the monitor and its row only shows once it is on;
        // with one, its row has the switch
        click(id("play_btn"))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            hasNotification(
                WeatherMonitorService.WEATHER_NOTIFICATION_ID,
                title = string(R.string.weather)
            )
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        hasText(id("play_bar_title"), "On · 30m")

        click(id("play_btn"))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
        }
    }

    private fun verifyQuickAction() {
        TestUtils.openQuickActions()
        click(quickAction(ToolIds.QUICK_ACTION_WEATHER_MONITOR))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            hasNotification(
                WeatherMonitorService.WEATHER_NOTIFICATION_ID,
                title = string(R.string.weather)
            )
        }

        // Wait for the battery restriction warning to go away
        optional {
            hasText(string(R.string.battery_settings_limit_accuracy))
            not { hasText(string(R.string.battery_settings_limit_accuracy)) }
        }

        click(quickAction(ToolIds.QUICK_ACTION_WEATHER_MONITOR))

        // TODO: Figure out how to check this on staging builds
        if (AutomationLibrary.packageName == null) {
            doesNotHaveNotification(WeatherMonitorService.WEATHER_NOTIFICATION_ID)
        }

        TestUtils.closeQuickActions()
    }

    // Seeds known pressure histories and verifies the forecast shown to the user, covering the whole
    // pipeline from stored readings to the weather screen.

    @Test
    fun steadyPressureHasNoForecastedChange() {
        openWithPressureTrend(1020f, 1020f, Duration.ofHours(6))

        hasText(string(R.string.weather_no_change))
        hasText(Regex(".*0\\.0 hPa / 3h.*"))
        hasText("1020.0", exact = true)
        not { hasText(string(R.string.weather_storm), exact = true, waitForTime = 0) }
    }

    @Test
    fun fallingPressureForecastsRain() {
        openWithPressureTrend(1018f, 1012f, Duration.ofHours(6))

        hasText(string(R.string.precipitation_rain))
        hasText(Regex(".*" + string(R.string.then_weather, "overcast")))
        // 6 hPa lost over 6 hours is a 3 hPa drop every 3 hours
        hasText(Regex(".*-?3\\.0 hPa / 3h.*"))
        hasText("1012.0", exact = true)
        // A moderate drop is not a storm
        not { hasText(string(R.string.weather_storm), exact = true, waitForTime = 0) }
    }

    @Test
    fun rapidlyFallingPressureShowsAStormAlert() {
        openWithPressureTrend(1020f, 1010f, Duration.ofHours(3))

        hasText(string(R.string.precipitation_rain))
        // The headline reads "Very soon, then ..."
        hasText(string(R.string.very_soon), ignoreCase = true)
        hasText(Regex(".*-?10\\.0 hPa / 3h.*"))
        hasText("1010.0", exact = true)

        // The alert is a row named after the alert, under the forecast
        hasText(string(R.string.weather_storm), exact = true)
        click(string(R.string.weather_storm), exact = true)
        hasText(string(R.string.weather_alert_storm_description))
        clickOk()
    }

    @Test
    fun risingPressureForecastsClearingSkies() {
        openWithPressureTrend(1008f, 1016f, Duration.ofHours(6))

        hasText(string(R.string.weather_wind))
        hasText(Regex(".*" + string(R.string.then_weather, "clear")))
        hasText(Regex(".*4\\.0 hPa / 3h.*"))
        hasText("1016.0", exact = true)
        not { hasText(string(R.string.weather_storm), exact = true, waitForTime = 0) }
    }

    @Test
    fun readingsSpanningUnderTenMinutesAreNotUsedToForecast() {
        // The same large drop, but in too short a period to be trusted
        openWithPressureTrend(
            1020f,
            1010f,
            Duration.ofMinutes(5),
            interval = Duration.ofMinutes(1)
        )

        hasText(string(R.string.weather_no_change))
        not { hasText(string(R.string.weather_storm), exact = true, waitForTime = 0) }
    }

    @Test
    fun pressureIsConvertedToTheSelectedUnits() {
        openWithPressureTrend(1013.25f, 1013.25f, Duration.ofHours(6), units = "in")

        hasText("29.92", exact = true)
        hasText("in", exact = true)
    }

    @Test
    fun pressureHistoryIsCharted() {
        openWithPressureTrend(1018f, 1012f, Duration.ofHours(6))

        isVisible(R.id.chart)
    }

    private fun setPressureUnits(units: String) {
        PreferencesSubsystem.getInstance(TestUtils.context).preferences.putString(
            PreferenceKeys.pressureUnits,
            units
        )
    }

    private fun openWithPressureTrend(
        start: Float,
        end: Float,
        duration: Duration,
        interval: Duration = Duration.ofMinutes(15),
        units: String = "hpa"
    ) {
        assumeFalse(isStagingBuild)
        assumeTrue(Tools.isToolAvailable(TestUtils.context, ToolIds.WEATHER))
        setPressureUnits(units)
        relaunchTool {
            TestData.addPressureTrend(start, end, duration, interval = interval)
        }
        // The historic temperature disclaimer can take a few seconds to open
        optional { clickOk(waitForTime = 4000) }
    }
}
