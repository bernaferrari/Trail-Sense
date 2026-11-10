package com.kylecorry.sol.math.regression

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.Polynomial

class NewtonPolynomialRegressionTest {

    @Test
    fun testFit() {
        testFit("2x + 1", " 2x + 1", 1)
        testFit("x^2 + 2x + 1", " x^2 + 2x + 1", 2)
        testFit("x^3 - 2x^2 + 3x - 4", " x^3 - 2x^2 + 3x - 4", 3)
        testFit("5", " 5", 1)
        testFit("x^2", " x^2", 2)
        testFit("2x - 3", " 2x - 3", 1)
        testFit("0.5x", " 0.5x", 1)
        testFit("x^4", " x^4", 4)
        testFit("x", " x", 1)
        testFit("x^3 + 2x^2 - x + 3", " x^3 + 2x^2 - x + 3", 3)
        testFit("x + 1", " x + 1", 1)
        testFit("3x^2 - 2x + 7", " 3x^2 - 2x + 7", 2)
        testFit("x^3", " x^3", 3)
        testFit("4x - 5", " 4x - 5", 1)
        // Lower order than required, it will approximate
        testFit("x^2 + 2x + 1", " 3x + 1", 1)
    }

    private fun testFit(sourcePolynomial: String, expectedPolynomial: String, order: Int) {
        val sourcePoly = Polynomial.of(sourcePolynomial)
        val points = (0..10).map { x -> Vector2(x.toFloat(), sourcePoly.evaluate(x.toFloat())) }

        val regression = NewtonPolynomialRegression(order)
        val polynomial = regression.fit(points)

        assertEquals(Polynomial.of(expectedPolynomial), polynomial)
    }
}
