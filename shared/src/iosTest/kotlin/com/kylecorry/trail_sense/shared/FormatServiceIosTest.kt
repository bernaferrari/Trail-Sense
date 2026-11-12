package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.IosPlatformFormats
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/** FormatService on iOS, reading the app's strings from Compose resources (the simulator runs in English). */
class FormatServiceIosTest {

    private val device = object : DeviceProfile {
        override val hasCompass = true
        override val hasGyroscope = true
        override val hasBarometer = true
        override val hasDynamicColors = false
        override val isMetricPreferred = true
        override val uses24HourClock = true
    }

    private val format = FormatService(
        ComposeAppStrings(),
        IosPlatformFormats(),
        UserPreferences(Preferences(MapSettings()) { {} }, device)
    )

    @Test
    fun readsStringsAndFillsPlaceholders() {
        assertEquals("5 m", format.formatDistance(Distance.meters(5f)))
        assertEquals("Clear", format.formatWeather(com.kylecorry.sol.science.meteorology.WeatherCondition.Clear))
    }

    @Test
    fun readsPlurals() {
        assertEquals("1 day", format.formatDays(1))
        assertEquals("3 days", format.formatDays(3))
    }

    @Test
    fun formatsDatesAndSizesNatively() {
        val date = format.formatDate(Clock.System.now())
        assertTrue(date.isNotBlank())
        assertTrue(format.formatFileSize(2_000_000).contains("MB"))
    }
}
