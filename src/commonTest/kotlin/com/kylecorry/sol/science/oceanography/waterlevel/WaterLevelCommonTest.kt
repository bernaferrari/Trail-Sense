package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.units.Coordinate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class WaterLevelCommonTest {
    @Test
    fun calculatesLunitidalHarmonic() {
        val calculator = HarmonicLunitidalWaterLevelCalculator(2.hours + 15.minutes, Coordinate(42.0, -72.0))
        assertEquals(0.9f, calculator.calculate(Instant.parse("2026-04-10T13:00:00Z"), TimeZone.UTC), 0.1f)
    }

    @Test
    fun piecewiseForwardsTimeZoneAndIncludesBoundaries() {
        val start = Instant.parse("2026-04-10T13:00:00Z")
        val end = start + 1.hours
        val zone = TimeZone.of("America/New_York")
        val calculator = object : IWaterLevelCalculator {
            override fun calculate(time: Instant, timeZone: TimeZone): Float = if (timeZone == zone) 1f else 2f
        }
        val piecewise = PiecewiseWaterLevelCalculator(listOf(Range(start, end) to calculator))
        assertEquals(1f, piecewise.calculate(start, zone))
        assertEquals(1f, piecewise.calculate(end, zone))
        assertEquals(0f, piecewise.calculate(end + 1.minutes, zone))
    }
}
