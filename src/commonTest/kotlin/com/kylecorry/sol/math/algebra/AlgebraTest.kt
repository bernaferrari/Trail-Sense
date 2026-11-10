package com.kylecorry.sol.math.algebra

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.Vector2

internal class AlgebraTest {

    @Test
    fun solveLinear() {
        assertEquals(-2.0f, Algebra.solve(LinearEquation(0.25f, 0.5f))!!, 0.0001f)
        assertEquals(0.0f, Algebra.solve(LinearEquation(0.0f, 0.0f))!!, 0.0001f)
        assertNull(Algebra.solve(LinearEquation(0.0f, 2.0f)))
    }

    @Test
    fun solveQuadratic() {
        assertEquals(0.0f to 0.0f, Algebra.solve(QuadraticEquation(1f, 0f, 0f))!!, 0.00001f)
        assertEquals(
            0.39039f to -0.64039f,
            Algebra.solve(QuadraticEquation(8f, 2f, -2f))!!,
            0.00001f
        )
        assertNull(Algebra.solve(QuadraticEquation(8f, -2f, 5f)))
    }

    @Test
    fun inverse() {
        val inverted = Algebra.inverse(LinearEquation(5f, 2f))
        assertEquals(0.2f to -0.4f, inverted.m to inverted.b, 0.0001f)
    }

    @Test
    fun getIntersection() {
        val intersection = Algebra.getIntersection(LinearEquation(2f, 1f), LinearEquation(-1f, 7f))

        assertEquals(Vector2(2f, 5f), intersection)
        assertNull(Algebra.getIntersection(LinearEquation(2f, 1f), LinearEquation(2f, 7f)))
    }

    @Test
    fun getIntersectionRequiresFiniteEquations() {
        assertFailsWith<IllegalArgumentException> {
            Algebra.getIntersection(LinearEquation(Float.NaN, 1f), LinearEquation(2f, 7f))
        }
        assertFailsWith<IllegalArgumentException> {
            Algebra.getIntersection(LinearEquation(1f, Float.POSITIVE_INFINITY), LinearEquation(2f, 7f))
        }
        assertFailsWith<IllegalArgumentException> {
            Algebra.getIntersection(LinearEquation(1f, 1f), LinearEquation(Float.NEGATIVE_INFINITY, 7f))
        }
        assertFailsWith<IllegalArgumentException> {
            Algebra.getIntersection(LinearEquation(1f, 1f), LinearEquation(2f, Float.NaN))
        }
    }

    @Test
    fun polynomial() {
        providePolynomial().forEach { args -> polynomial(args[0].arg<Double>(), args[1].arg<DoubleArray>(), args[2].arg<Double>()) }
    }

    private fun polynomial(x: Double, coefficients: DoubleArray, expected: Double) {
        val actual = Algebra.polynomial(x, *coefficients)
        assertEquals(expected, actual, 0.00001)
    }

    fun assertEquals(
        expected: Pair<Float, Float>,
        actual: Pair<Float, Float>,
        tolerance: Float = 0.0f
    ) {
        assertEquals(expected.first, actual.first, tolerance)
        assertEquals(expected.second, actual.second, tolerance)
    }

    companion object {
        fun providePolynomial(): List<List<Any?>> {
            return listOf(
                listOf(1.0, doubleArrayOf(1.0, 2.0, 3.0), 6.0),
                listOf(2.0, doubleArrayOf(1.0, 2.0, 3.0), 17.0),
                listOf(3.0, doubleArrayOf(0.0, 1.0, 3.0, 1.0), 57.0),
                listOf(3.0, doubleArrayOf(0.0, 1.0, -3.0, 1.0), 3.0),
                listOf(3.0, doubleArrayOf(), 0.0),
                listOf(3.0, doubleArrayOf(1.0), 1.0),
            )
        }
    }
}
