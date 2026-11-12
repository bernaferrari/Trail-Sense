package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class ActiveTimeCalculatorTest {

    private val calculator = ActiveTimeCalculator()

    @Test
    fun calculate() {
        val cases = listOf(
            Triple(0L, 10.seconds, Duration.ZERO),
            Triple(-1L, 10.seconds, Duration.ZERO),
            Triple(10L, Duration.ZERO, Duration.ZERO),
            Triple(10L, (-1).seconds, Duration.ZERO),
            Triple(10L, 10.seconds, 10.seconds),
            Triple(10L, 1.minutes, 20.seconds),
            Triple(10L, 15.seconds, 15.seconds)
        )
        for ((steps, elapsed, expected) in cases) {
            assertEquals(expected, calculator.calculate(steps, elapsed), "$steps steps in $elapsed")
        }
    }
}
