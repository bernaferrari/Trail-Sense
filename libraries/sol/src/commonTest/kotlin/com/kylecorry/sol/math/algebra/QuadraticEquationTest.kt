package com.kylecorry.sol.math.algebra

import kotlin.test.Test
import kotlin.test.assertEquals

internal class QuadraticEquationTest {

    @Test
    fun evaluate() {
        assertEquals(21.0f, QuadraticEquation(2f, 1 / 3f, 2f).evaluate(3.0f), 0.0001f)
    }
}