package com.kylecorry.sol.science.oceanography.waterlevel

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.tests.zoned
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class HarmonicLunitidalWaterLevelCalculatorTest {

    @Test
    fun calculate() {
        val calculator = HarmonicLunitidalWaterLevelCalculator(
            2.hours + 15.minutes,
            Coordinate(42.0, -72.0)
        )

        assertEquals(
            0.9f,
            calculator.calculate(zoned("2026-04-10T09:00:00-04:00[America/New_York]")),
            0.1f
        )
        assertEquals(
            0.3f,
            calculator.calculate(zoned("2026-04-10T12:00:00-04:00[America/New_York]")),
            0.1f
        )
        assertEquals(
            -0.9f,
            calculator.calculate(zoned("2026-04-10T15:00:00-04:00[America/New_York]")),
            0.1f
        )
        assertEquals(
            -0.4f,
            calculator.calculate(zoned("2026-04-10T18:00:00-04:00[America/New_York]")),
            0.1f
        )
    }
}
