package com.kylecorry.trail_sense.tools.tides

import com.kylecorry.trail_sense.tools.tides.infrastructure.model.TideTableWaterLevelCalculator
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDateTime
import kotlin.time.Instant
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.test_utils.TestUtils.context
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideTableWaterLevelCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

internal class TideTableWaterLevelCalculatorTest {

    @Test
    fun calculate() {
        val table = TideTable(
            0, listOf(
                Tide.high(time(10, 1, 42), 3.25f, TimeZone.UTC),
                Tide.high(time(10, 14, 0), 2.71f, TimeZone.UTC),
                Tide.low(time(10, 19, 27), 0.39f, TimeZone.UTC),
                Tide.high(time(11, 14, 56), 2.54f, TimeZone.UTC),
                Tide.low(time(11, 20, 20), 0.4f, TimeZone.UTC),
            )
        )

        val calculator = TideTableWaterLevelCalculator(context, table)

        check(calculator, time(9, 13, 7), 3.25f)
        check(calculator, time(9, 18, 33), 0.39f)
        check(calculator, time(10, 1, 42), 3.25f, true)
        check(calculator, time(10, 8, 23), 0.4f)
        check(calculator, time(10, 14, 0), 2.71f, true)
        check(calculator, time(10, 19, 27), 0.39f, true)
        check(calculator, time(11, 2, 37), 2.54f)
        check(calculator, time(11, 9, 25), 0.39f)
        check(calculator, time(11, 14, 56), 2.54f, true)
        check(calculator, time(11, 20, 20), 0.4f, true)
        check(calculator, time(12, 3, 37), 3.07f)
        check(calculator, time(12, 10, 11), 0.56f)
    }

    @Test
    fun calculateOutsideSupportedYears() {
        val table = TideTable(
            0, listOf(
                Tide.high(utc(1990, 1, 1, 0, 0), 3f, TimeZone.UTC),
                Tide.low(utc(1990, 1, 1, 6, 0), 1f, TimeZone.UTC),
            )
        )

        val calculator = TideTableWaterLevelCalculator(context, table)

        assertEquals(
            0f,
            calculator.calculate(utc(1990, 1, 1, 3, 0), TimeZone.UTC),
            0.0001f
        )
        assertEquals(
            0f,
            calculator.calculate(utc(3001, 1, 1, 0, 0), TimeZone.UTC),
            0.0001f
        )
    }

    private fun check(
        calculator: TideTableWaterLevelCalculator,
        time: Instant,
        height: Float,
        exact: Boolean = false
    ) {
        assertEquals(height, calculator.calculate(time, TimeZone.UTC), if (exact) 0.0001f else 0.2f)
    }

    private fun time(day: Int, hour: Int, minute: Int): Instant {
        return utc(2022, 1, day, hour, minute)
    }

    private fun utc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant {
        return LocalDateTime(year, month, day, hour, minute).toInstant(TimeZone.UTC)
    }
}
