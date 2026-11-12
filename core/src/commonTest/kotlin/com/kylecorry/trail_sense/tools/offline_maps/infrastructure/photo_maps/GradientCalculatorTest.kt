package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GradientCalculatorTest {

    private val calculator = GradientCalculator()

    @Test
    fun uniformImageHasNoGradient() {
        val gradients = calculator.calculate(IntArray(SIZE * SIZE) { GRAY }, SIZE, SIZE)

        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                assertEquals(0f, gradients.magnitude[y, x])
            }
        }
    }

    @Test
    fun verticalEdgeHasHorizontalGradient() {
        val pixels = IntArray(SIZE * SIZE) { i -> if (i % SIZE < SIZE / 2) BLACK else WHITE }

        val gradients = calculator.calculate(pixels, SIZE, SIZE)

        val y = SIZE / 2
        val x = SIZE / 2
        assertTrue(gradients.x[y, x] > 0f, "dark to light is a positive x gradient")
        assertEquals(0f, gradients.y[y, x])
        assertTrue(gradients.magnitude[y, x] > 0f)
    }

    @Test
    fun alphaDoesNotAffectTheGradient() {
        val opaque = IntArray(SIZE * SIZE) { i -> if (i % SIZE < SIZE / 2) BLACK else WHITE }
        val transparent = IntArray(opaque.size) { opaque[it] and 0x00FFFFFF }

        val a = calculator.calculate(opaque, SIZE, SIZE)
        val b = calculator.calculate(transparent, SIZE, SIZE)

        assertEquals(a.magnitude[SIZE / 2, SIZE / 2], b.magnitude[SIZE / 2, SIZE / 2])
    }

    private companion object {
        const val SIZE = 12
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val GRAY = 0xFF808080.toInt()
    }
}
