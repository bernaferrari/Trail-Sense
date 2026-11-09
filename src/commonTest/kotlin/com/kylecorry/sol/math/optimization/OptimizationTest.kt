package com.kylecorry.sol.math.optimization

import kotlin.test.Test
import kotlin.test.assertEquals

import kotlin.math.sqrt

internal class OptimizationTest {

    @Test
    fun newtonRaphson() {
        newtonRaphson(2.0)
        newtonRaphson(9.0)
    }

    private fun newtonRaphson(value: Double) {
        val result = Optimization.newtonRaphsonIteration(initialValue = 1.0) { x ->
            (x + value / x) / 2.0
        }
        assertEquals(sqrt(value), result, 1e-6)
    }

    @Test
    fun newtonRaphsonFloat() {
        newtonRaphsonFloat(2f)
        newtonRaphsonFloat(9f)
    }

    private fun newtonRaphsonFloat(value: Float) {
        val result = Optimization.newtonRaphsonIteration(initialValue = 1f) { x ->
            (x + value / x) / 2f
        }
        assertEquals(sqrt(value), result, 1e-6f)
    }

    @Test
    fun newtonRaphsonRespectsMaxIterations() {
        var callCount = 0
        Optimization.newtonRaphsonIteration(initialValue = 1.0, maxIterations = 5) { x ->
            callCount++
            (x + 2.0 / x) / 2.0
        }
        assertEquals(5, callCount)
    }

    @Test
    fun newtonRaphsonRespectsTolerance() {
        var callCount = 0
        val result = Optimization.newtonRaphsonIteration(initialValue = 1.0, maxIterations = 5, tolerance = 1.0) { x ->
            callCount++
            (x + 2.0 / x) / 2.0
        }
        assertEquals(1, callCount)
        assertEquals(sqrt(2.0), result, 1.0)

    }
}
