package com.kylecorry.sol.math.optimization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import com.kylecorry.sol.math.Range

internal class ConvergenceOptimizerTest {

    @Test
    fun optimizeRequiresFinitePositiveInitialStep() {
        val optimizer = ConvergenceOptimizer(
            Float.POSITIVE_INFINITY,
            0.1f
        ) { _, _ ->
            GridSearchOptimizer(0.1)
        }

        assertFailsWith<IllegalArgumentException> {
            optimizer.optimize(Range(-1.0, 1.0), Range(-1.0, 1.0), false) { x, y -> x + y }
        }
    }

    @Test
    fun optimizeRequiresPositiveFinitePrecision() {
        val optimizer = ConvergenceOptimizer(
            1f,
            0f
        ) { _, _ ->
            GridSearchOptimizer(0.1)
        }

        assertFailsWith<IllegalArgumentException> {
            optimizer.optimize(Range(-1.0, 1.0), Range(-1.0, 1.0), false) { x, y -> x + y }
        }
    }

    @Test
    fun optimize() {
        val optimizer = ConvergenceOptimizer(
            4f,
            0.25f
        ) { step, _ ->
            GridSearchOptimizer(step.toDouble())
        }

        val result = optimizer.optimize(Range(-10.0, 10.0), Range(-10.0, 10.0), false) { x, y ->
            x * x + y * y
        }

        assertEquals(0.0, result.first, 0.5)
        assertEquals(0.0, result.second, 0.5)
    }
}
