package com.kylecorry.trail_sense.tools.solarpanel

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.GPS_WAIT_FOR_TIMEOUT
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.ZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.solarpanel.domain.SolarPanelService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import kotlin.time.Instant

class ToolSolarPanelAlignerTest : ToolTestBase(Tools.SOLAR_PANEL_ALIGNER, location) {

    override fun seedData() {
        // A fixed sunny moment (local noon on the June solstice at the test location), so both "Today" and the
        // 2h "Duration of charge" have sun to estimate at whatever hour the test runs
        SolarPanelService.defaultTimeProvider = { ZonedDateTime(noon, zone) }
    }

    @After
    fun restoreClock() {
        SolarPanelService.defaultTimeProvider = SystemZonedDateTimeProvider()
    }

    @Test
    fun verifyBasicFunctionality() {
        if (!Tools.isToolAvailable(context, Tools.SOLAR_PANEL_ALIGNER)) {
            return
        }

        // It should show the solar panel details for today
        hasText(string(R.string.tilt), waitForTime = GPS_WAIT_FOR_TIMEOUT)
        hasText(Regex("-?\\d+°"))
        hasText(Regex("-?\\d+°"), index = 1)

        hasText(string(R.string.compass_azimuth))
        hasText(Regex("\\d+°"))
        hasText(Regex("\\d+°"), index = 1)

        hasText(Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that today is selected

        // It should show the solar panel details for a custom time
        click(string(R.string.duration_of_charge), exact = true)

        clickOk()

        hasText(string(R.string.tilt))
        hasText(Regex("-?\\d+°"))
        hasText(Regex("-?\\d+°"), index = 1)

        hasText(string(R.string.compass_azimuth))
        hasText(Regex("\\d+°"))
        hasText(Regex("\\d+°"), index = 1)

        hasText(Regex("Up to \\d+(\\.\\d+)? kWh / m²"))

        // TODO: Verify that the now button is selected
        hasText("2h")
    }
}

private val location = Coordinate(42.03, -71.97)
private val zone = TimeZone.of("America/New_York")
private val noon = Instant.parse("2026-06-21T16:00:00Z")
