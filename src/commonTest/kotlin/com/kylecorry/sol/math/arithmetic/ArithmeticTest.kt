package com.kylecorry.sol.math.arithmetic

import kotlin.test.Test
import kotlin.test.assertEquals

class ArithmeticTest {
    @Test
    fun factorial() {
        factorial(0, 1L)
        factorial(1, 1L)
        factorial(2, 2L)
        factorial(3, 6L)
        factorial(4, 24L)
        factorial(5, 120L)
        factorial(20, 2432902008176640000L)
        factorial(-1, -1L)
        factorial(-5, -120L)
        factorial(-20, -2432902008176640000L)
    }

    private fun factorial(n: Int, expected: Long) {
        assertEquals(expected, Arithmetic.factorial(n))
    }

    @Test
    fun wrapDouble() {
        wrapDouble(0.2, 0.0, 1.0, 0.2)
        wrapDouble(1.0, 0.0, 1.0, 1.0)
        wrapDouble(0.0, 0.0, 1.0, 0.0)
        wrapDouble(1.5, 0.0, 1.0, 0.5)
        wrapDouble(-0.75, 0.0, 1.0, 0.25)
        wrapDouble(0.0, 1.0, 4.0, 3.0)
        wrapDouble(5.0, 1.0, 4.0, 2.0)
        wrapDouble(6.0, 5.0, 4.0, 5.0)
        wrapDouble(0.0, 0.0, 0.0, 0.0)
        wrapDouble(1800.0, 0.0, 360.0, 0.0)
        wrapDouble(-1800.0, 0.0, 360.0, 360.0)
        wrapDouble(1799.0, 0.0, 360.0, 359.0)
        wrapDouble(-1799.0, 0.0, 360.0, 1.0)
    }

    private fun wrapDouble(value: Double, min: Double, max: Double, expected: Double) {
        val actual = Arithmetic.wrap(value, min, max)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun wrapFloat() {
        wrapFloat(0.2f, 0.0f, 1.0f, 0.2f)
        wrapFloat(1.0f, 0.0f, 1.0f, 1.0f)
        wrapFloat(0.0f, 0.0f, 1.0f, 0.0f)
        wrapFloat(1.5f, 0.0f, 1.0f, 0.5f)
        wrapFloat(-0.75f, 0.0f, 1.0f, 0.25f)
        wrapFloat(0.0f, 1.0f, 4.0f, 3.0f)
        wrapFloat(5.0f, 1.0f, 4.0f, 2.0f)
        wrapFloat(6.0f, 5.0f, 4.0f, 5.0f)
        wrapFloat(0.0f, 0.0f, 0.0f, 0.0f)
        wrapFloat(1800.0f, 0.0f, 360.0f, 0.0f)
        wrapFloat(-1800.0f, 0.0f, 360.0f, 360.0f)
        wrapFloat(1799.0f, 0.0f, 360.0f, 359.0f)
        wrapFloat(-1799.0f, 0.0f, 360.0f, 1.0f)
        wrapFloat(-250.0f, -180.0f, 180.0f, 110.0f)
    }

    private fun wrapFloat(value: Float, min: Float, max: Float, expected: Float) {
        val actual = Arithmetic.wrap(value, min, max)
        assertEquals(expected, actual, 0.00001f)
    }

    @Test
    fun power() {
        power(1.0, 2, 1.0)
        power(1.0, 0, 1.0)
        power(1.0, -1, 1.0)
        power(3.0, -1, 0.3333333333333333)
        power(3.0, -2, 0.1111111111111111)
        power(3.0, 0, 1.0)
        power(3.0, 1, 3.0)
        power(3.0, 2, 9.0)
        power(0.0, 2, 0.0)
        power(-2.0, 2, 4.0)
        power(-2.0, 3, -8.0)
        power(0.5, 2, 0.25)
        power(0.5, -2, 4.0)
    }

    private fun power(value: Double, exponent: Int, expected: Double) {
        val actual = Arithmetic.power(value, exponent)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun powerInt() {
        powerInt(1, 2, 1)
        powerInt(1, 0, 1)
        powerInt(1, -1, 1)
        powerInt(3, -1, 0)
        powerInt(3, -2, 0)
        powerInt(3, 0, 1)
        powerInt(3, 1, 3)
        powerInt(3, 2, 9)
        powerInt(-2, 2, 4)
        powerInt(-2, 3, -8)
        powerInt(0, 3, 0)
    }

    private fun powerInt(value: Int, exponent: Int, expected: Int) {
        val actual = Arithmetic.power(value, exponent)
        assertEquals(expected, actual)
    }

    @Test
    fun cube() {
        cube(1.0, 1.0)
        cube(2.0, 8.0)
        cube(3.0, 27.0)
        cube(4.0, 64.0)
        cube(-4.0, -64.0)
        cube(0.0, 0.0)
        cube(0.5, 0.125)
    }

    private fun cube(x: Double, expected: Double) {
        val actual = Arithmetic.cube(x)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun square() {
        square(1.0, 1.0)
        square(2.0, 4.0)
        square(3.0, 9.0)
        square(4.0, 16.0)
        square(-4.0, 16.0)
        square(0.0, 0.0)
        square(0.5, 0.25)
    }

    private fun square(x: Double, expected: Double) {
        val actual = Arithmetic.square(x)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun clampDouble() {
        clampDouble(0.1, 0.0, 1.0, 0.1)
        clampDouble(0.0, 0.0, 1.0, 0.0)
        clampDouble(1.0, 0.0, 1.0, 1.0)
        clampDouble(1.2, 0.0, 1.0, 1.0)
        clampDouble(-0.1, 0.0, 1.0, 0.0)
        clampDouble(4.0, 2.0, 5.0, 4.0)
        clampDouble(1.0, 2.0, 5.0, 2.0)
        clampDouble(6.0, 2.0, 5.0, 5.0)
    }

    private fun clampDouble(value: Double, min: Double, max: Double, expected: Double) {
        val actual = Arithmetic.clamp(value, min, max)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun clampFloat() {
        clampFloat(0.1, 0.0, 1.0, 0.1)
        clampFloat(0.0, 0.0, 1.0, 0.0)
        clampFloat(1.0, 0.0, 1.0, 1.0)
        clampFloat(1.2, 0.0, 1.0, 1.0)
        clampFloat(-0.1, 0.0, 1.0, 0.0)
        clampFloat(4.0, 2.0, 5.0, 4.0)
        clampFloat(1.0, 2.0, 5.0, 2.0)
        clampFloat(6.0, 2.0, 5.0, 5.0)
    }

    private fun clampFloat(value: Double, min: Double, max: Double, expected: Double) {
        val actual = Arithmetic.clamp(value.toFloat(), min.toFloat(), max.toFloat())
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    @Test
    fun greatestCommonDivisor() {
        greatestCommonDivisor(1, 1, 1)
        greatestCommonDivisor(100, 200, 100)
        greatestCommonDivisor(200, 100, 100)
        greatestCommonDivisor(0, 0, 0)
        greatestCommonDivisor(0, 1, 1)
        greatestCommonDivisor(1, 0, 1)
        greatestCommonDivisor(-1, 1, 1)
        greatestCommonDivisor(400, 600, 200)
    }

    private fun greatestCommonDivisor(a: Int, b: Int, expected: Int) {
        val actual = Arithmetic.greatestCommonDivisor(a, b)
        assertEquals(expected, actual)
    }

    @Test
    fun leastCommonMultiple() {
        leastCommonMultiple(1, 1, 1)
        leastCommonMultiple(100, 200, 200)
        leastCommonMultiple(200, 100, 200)
        leastCommonMultiple(0, 0, 0)
        leastCommonMultiple(0, 1, 0)
        leastCommonMultiple(1, 0, 0)
        leastCommonMultiple(-1, 1, 1)
        leastCommonMultiple(400, 600, 1200)
        leastCommonMultiple(3800, 7600, 7600)
    }

    private fun leastCommonMultiple(a: Int, b: Int, expected: Int) {
        val actual = Arithmetic.leastCommonMultiple(a, b)
        assertEquals(expected, actual)
    }

    @Test
    fun greatestCommonDivisorFloat() {
        greatestCommonDivisorFloat(1.0f, 1.0f, 1.0f)
        greatestCommonDivisorFloat(1.5f, 1.0f, 0.5f)
        greatestCommonDivisorFloat(1.4f, 1.0f, 0.2f)
        greatestCommonDivisorFloat(0.01f, 3.8f, 0.01f)
        greatestCommonDivisorFloat(0.0f, 1.0f, 1.0f)
        greatestCommonDivisorFloat(1.0f, 0.0f, 1.0f)
        greatestCommonDivisorFloat(0.0f, 0.0f, 0.0f)
    }

    private fun greatestCommonDivisorFloat(a: Float, b: Float, expected: Float) {
        val actual = Arithmetic.greatestCommonDivisor(a, b)
        assertEquals(expected, actual, 0.001f)
    }

    @Test
    fun leastCommonMultipleFloat() {
        leastCommonMultipleFloat(1.0f, 1.0f, 1.0f)
        leastCommonMultipleFloat(1.5f, 1.0f, 3.0f)
        leastCommonMultipleFloat(1.4f, 1.0f, 7.0f)
        leastCommonMultipleFloat(0.01f, 3.8f, 3.8f)
        leastCommonMultipleFloat(0.0f, 1.0f, 0.0f)
        leastCommonMultipleFloat(1.0f, 0.0f, 0.0f)
        leastCommonMultipleFloat(0.0f, 0.0f, 0.0f)
    }

    private fun leastCommonMultipleFloat(a: Float, b: Float, expected: Float) {
        val actual = Arithmetic.leastCommonMultiple(a, b)
        assertEquals(expected, actual, 0.001f)
    }
}
