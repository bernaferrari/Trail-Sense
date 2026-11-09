package com.kylecorry.sol.math.algebra

import kotlin.test.Test
import kotlin.test.assertEquals

internal class LinearEquationTest {

    @Test
    fun evaluate() {
        assertEquals(1.0f, LinearEquation(0.25f, 0.5f).evaluate(2.0f), 0.0001f)
    }
}