package com.kylecorry.sol.science.oceanography.waterlevel

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.science.oceanography.high
import com.kylecorry.sol.science.oceanography.low

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.tests.ZonedTime
import com.kylecorry.sol.tests.zoned

internal class RuleOfTwelfthsWaterLevelCalculatorTest {

    @Test
    fun calculate() {
        provideWaterLevels().forEach { args -> calculate(args[0].arg<Tide>(), args[1].arg<Tide>(), args[2].arg<ZonedTime>(), args[3].arg<Float>()) }
    }

    private fun calculate(
        first: Tide,
        second: Tide,
        time: ZonedTime,
        expected: Float
    ) {
        val calculator = RuleOfTwelfthsWaterLevelCalculator(first, second)
        val actual = calculator.calculate(time)
        assertEquals(expected, actual, 0.01f)
    }

    companion object {
        fun provideWaterLevels(): List<List<Any?>> {
            return listOf(
                // Low to high
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 0, 0),
                    0f
                ),
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 1, 0),
                    0.8f
                ),
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 2, 0),
                    3f
                ),
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 3, 0),
                    6f
                ),
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 4, 0),
                    9f
                ),
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 5, 0),
                    11.2f
                ),
                listOf(
                    Tide.low(time(1, 0, 0), 0f),
                    Tide.high(time(1, 6, 0), 12f),
                    time(1, 6, 0),
                    12f
                ),
                // High to low
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 6, 0),
                    12f
                ),
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 7, 0),
                    11.2f
                ),
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 8, 0),
                    9f
                ),
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 9, 0),
                    6f
                ),
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 10, 0),
                    3f
                ),
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 11, 0),
                    0.8f
                ),
                listOf(
                    Tide.high(time(1, 6, 0), 12f),
                    Tide.low(time(1, 12, 0), 0f),
                    time(1, 12, 0),
                    0f
                ),
            )
        }

        private fun time(day: Int, hour: Int, minute: Int): ZonedTime {
            return zoned(2022, 1, day, hour, minute)
        }

    }
}