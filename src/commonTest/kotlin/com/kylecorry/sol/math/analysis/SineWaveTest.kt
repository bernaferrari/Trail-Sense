package com.kylecorry.sol.math.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.math.trigonometry.SineWave

class SineWaveTest {

    @Test
    fun calculate() {
        calculate(1f, 1f, 0f, 0f, 0f, 0f)
        calculate(1f, 1f, 0f, 0f, 1f, 0.8415f)
        calculate(1f, 1f, 0f, 0f, 2f, 0.9093f)
        calculate(1f, 1f, 0f, 0f, 3f, 0.1411f)
        calculate(1f, 1f, 0f, 0f, 4f, -0.7568f)
        calculate(1f, 1f, 0f, 0f, 5f, -0.9589f)
        calculate(1f, 1f, 0f, 0f, 6f, -0.2794f)
        calculate(1f, 1f, 0f, 0f, 7f, 0.657f)
        calculate(1f, 1f, 0f, 0f, 8f, 0.9894f)
        calculate(1f, 1f, 0f, 0f, 9f, 0.4121f)
        calculate(1f, 1f, 0f, 0f, 10f, -0.544f)
        calculate(1f, 1f, 1f, 2f, 0f, 1.1585f)
        calculate(2f, 4f, 1f, 1f, 0f, 2.5136f)
    }

    private fun calculate(amplitude: Float, frequency: Float, horizontalShift: Float, verticalShift: Float, x: Float, expected: Float) {
        val wave = SineWave(amplitude, frequency, horizontalShift, verticalShift)
        val actual = wave.calculate(x)
        assertEquals(expected, actual, 0.0001f)
    }
}