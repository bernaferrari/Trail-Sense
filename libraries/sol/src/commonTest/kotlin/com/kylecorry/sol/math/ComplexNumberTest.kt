package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexNumberTest {

    @Test
    fun getPhase() {
        getPhase(1f, 1f, 0.7853981633974483f)
        getPhase(1f, 0f, 0.0f)
        getPhase(0f, 1f, 1.5707963267948966f)
        getPhase(-1f, 0f, 3.141592653589793f)
        getPhase(0f, -1f, -1.5707963267948966f)
        getPhase(-1f, -1f, -2.356194490192345f)
    }

    private fun getPhase(real: Float, imaginary: Float, expected: Float) {
        val complex = ComplexNumber(real, imaginary)
        assertEquals(expected, complex.phase, 0.0001f)
    }

    @Test
    fun getMagnitude() {
        getMagnitude(1f, 1f, 1.4142135623730951f)
        getMagnitude(1f, 0f, 1.0f)
        getMagnitude(0f, 1f, 1.0f)
        getMagnitude(-1f, 0f, 1.0f)
        getMagnitude(0f, -1f, 1.0f)
        getMagnitude(-1f, -1f, 1.4142135623730951f)
    }

    private fun getMagnitude(real: Float, imaginary: Float, expected: Float) {
        val complex = ComplexNumber(real, imaginary)
        assertEquals(expected, complex.magnitude, 0.0001f)
    }

    @Test
    fun plus() {
        plus(1f, 1f, 1f, 1f, 2f, 2f)
        plus(1f, 1f, 1f, -1f, 2f, 0f)
        plus(1f, 1f, -1f, 1f, 0f, 2f)
        plus(1f, 1f, -1f, -1f, 0f, 0f)
    }

    private fun plus(real1: Float, imaginary1: Float, real2: Float, imaginary2: Float, expectedReal: Float, expectedImaginary: Float) {
        val complex1 = ComplexNumber(real1, imaginary1)
        val complex2 = ComplexNumber(real2, imaginary2)
        val expected = ComplexNumber(expectedReal, expectedImaginary)
        assertEquals(expected, complex1 + complex2)
    }

    @Test
    fun minus() {
        minus(1f, 1f, 1f, 1f, 0f, 0f)
        minus(1f, 1f, 1f, -1f, 0f, 2f)
        minus(1f, 1f, -1f, 1f, 2f, 0f)
        minus(1f, 1f, -1f, -1f, 2f, 2f)
    }

    private fun minus(real1: Float, imaginary1: Float, real2: Float, imaginary2: Float, expectedReal: Float, expectedImaginary: Float) {
        val complex1 = ComplexNumber(real1, imaginary1)
        val complex2 = ComplexNumber(real2, imaginary2)
        val expected = ComplexNumber(expectedReal, expectedImaginary)
        assertEquals(expected, complex1 - complex2)
    }

    @Test
    fun times() {
        times(1f, 1f, 1f, 1f, 0f, 2f)
        times(1f, 1f, 1f, -1f, 2f, 0f)
        times(1f, 1f, -1f, 1f, -2f, 0f)
        times(1f, 1f, -1f, -1f, 0f, -2f)
    }

    private fun times(real1: Float, imaginary1: Float, real2: Float, imaginary2: Float, expectedReal: Float, expectedImaginary: Float) {
        val complex1 = ComplexNumber(real1, imaginary1)
        val complex2 = ComplexNumber(real2, imaginary2)
        val expected = ComplexNumber(expectedReal, expectedImaginary)
        assertEquals(expected, complex1 * complex2)
    }

    @Test
    fun timesScalar() {
        timesScalar(1f, 1f, 2f, 2f, 2f)
        timesScalar(1f, 1f, -1f, -1f, -1f)
        timesScalar(1f, 1f, 0f, 0f, 0f)
        timesScalar(-1f, 1f, -1f, 1f, -1f)
    }

    private fun timesScalar(real: Float, imaginary: Float, scalar: Float, expectedReal: Float, expectedImaginary: Float) {
        val complex = ComplexNumber(real, imaginary)
        val expected = ComplexNumber(expectedReal, expectedImaginary)
        assertEquals(expected, complex * scalar)
    }

    @Test
    fun conjugate() {
        conjugate(1f, 1f, 1f, -1f)
        conjugate(1f, -1f, 1f, 1f)
        conjugate(-1f, 1f, -1f, -1f)
        conjugate(-1f, -1f, -1f, 1f)
    }

    private fun conjugate(real: Float, imaginary: Float, expectedReal: Float, expectedImaginary: Float) {
        val complex = ComplexNumber(real, imaginary)
        val expected = ComplexNumber(expectedReal, expectedImaginary)
        assertEquals(expected, complex.conjugate())
    }

    @Test
    fun exp() {
        exp(0f, 1f, 0f)
        exp(3.141592653589793f, -1f, 0f)
        exp(1.5707963267948966f, 0f, 1f)
        exp(4.71238898038469f, 0f, -1f)
        exp(6.283185307179586f, 1f, 0f)
    }

    private fun exp(theta: Float, expectedReal: Float, expectedImaginary: Float) {
        val expected = ComplexNumber(expectedReal, expectedImaginary)
        assertEquals(expected.real, ComplexNumber.exp(theta).real, 0.001f)
        assertEquals(expected.imaginary, ComplexNumber.exp(theta).imaginary, 0.001f)
    }
}