package com.kylecorry.sol.science.oceanography.waterlevel

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.science.oceanography.high
import com.kylecorry.sol.science.oceanography.low

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.tests.ZonedTime
import com.kylecorry.sol.tests.zoned
import kotlin.math.sqrt

internal class TideClockWaterLevelCalculatorTest {

    @Test
    fun calculate() {
        provideTideClock().forEach { args -> calculate(args[0].arg<Tide>(), args[1].arg<Float>(), args[2].arg<Float>(), args[3].arg<Float>(), args[4].arg<ZonedTime>(), args[5].arg<Float>()) }
    }

    private fun calculate(
        reference: Tide,
        frequency: Float,
        amplitude: Float,
        z0: Float,
        time: ZonedTime,
        expected: Float
    ) {
        val calculator = TideClockWaterLevelCalculator(reference, frequency, amplitude, z0)
        val actual = calculator.calculate(time)
        assertEquals(expected, actual, 0.01f)
    }

    companion object {
        fun provideTideClock(): List<List<Any?>> {
            return listOf(
                // High reference
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 0), 1f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 0), -1f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 2, 0), 1f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 30), 0f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 30), 0f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(9, 23, 0), -1f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(9, 22, 0), 1f),

                // Low reference
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 0), -1f),
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 0), 1f),
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 2, 0), -1f),
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 30), 0f),
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 30), 0f),
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(9, 23, 0), 1f),
                listOf(Tide.low(time(10, 0, 0)), 180f, 1f, 0f, time(9, 22, 0), -1f),

                // Z0
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 5f, time(10, 0, 0), 6f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 5f, time(10, 1, 0), 4f),

                // Amplitude
                listOf(Tide.high(time(10, 0, 0)), 180f, 2f, 0f, time(10, 0, 0), 2f),
                listOf(Tide.high(time(10, 0, 0)), 180f, 2f, 0f, time(10, 1, 0), -2f),

                // Different frequency
                listOf(Tide.high(time(10, 0, 0)), 90f, 1f, 0f, time(10, 0, 0), 1f),
                listOf(Tide.high(time(10, 0, 0)), 90f, 1f, 0f, time(10, 1, 0), 0f),
                listOf(Tide.high(time(10, 0, 0)), 90f, 1f, 0f, time(10, 2, 0), -1f),

                // Intermediate
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 0, 15), sqrt(2f) / 2),
                listOf(Tide.high(time(10, 0, 0)), 180f, 1f, 0f, time(10, 1, 15), -sqrt(2f) / 2),

                // Different reference date
                listOf(Tide.high(time(1, 0, 0)), 90f, 1f, 0f, time(2, 0, 0), 1f),
                listOf(Tide.high(time(1, 0, 0)), 90f, 1f, 0f, time(2, 1, 0), 0f),
                listOf(Tide.high(time(1, 0, 0)), 90f, 1f, 0f, time(2, 2, 0), -1f),
            )
        }

        private fun time(day: Int, hour: Int, minute: Int): ZonedTime {
            return zoned(2022, 1, day, hour, minute)
        }
    }

}