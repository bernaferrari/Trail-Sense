package com.kylecorry.sol.science.astronomy.rst

import com.kylecorry.sol.science.astronomy.locators.SunModel
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class SearchRiseSetModelCommonTest {
    @Test
    fun findsNewYorkSunEvents() {
        val times = SearchRiseSetModel().calculate(
            SunModel(), LocalDate(2020, 9, 12), TimeZone.of("America/New_York"),
            Coordinate(40.7128, -74.0060), -0.8333, true, false
        )
        val rise = assertNotNull(times.rise)
        val transit = assertNotNull(times.transit)
        val set = assertNotNull(times.set)
        assertTrue((rise - Instant.parse("2020-09-12T10:34:00Z")).absoluteValue < 5.minutes)
        assertTrue((transit - Instant.parse("2020-09-12T16:52:00Z")).absoluteValue < 5.minutes)
        assertTrue((set - Instant.parse("2020-09-12T23:09:00Z")).absoluteValue < 5.minutes)
    }
}
