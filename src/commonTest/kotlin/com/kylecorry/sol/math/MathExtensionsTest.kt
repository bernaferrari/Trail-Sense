package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.MathExtensions.negative
import com.kylecorry.sol.math.MathExtensions.positive
import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.math.MathExtensions.round
import com.kylecorry.sol.math.MathExtensions.roundNearest
import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.MathExtensions.toRadians

class MathExtensionsTest {

    @Test
    fun toRadiansDouble() {
        provideRadians().forEach { args -> toRadiansDouble(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun toRadiansDouble(angle: Double, expected: Double) {
        val actual = angle.toRadians()
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun toRadiansFloat() {
        provideRadians().forEach { args -> toRadiansFloat(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun toRadiansFloat(angle: Double, expected: Double) {
        val actual = angle.toFloat().toRadians()
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    @Test
    fun toDegreesDouble() {
        provideRadians().forEach { args -> toDegreesDouble(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun toDegreesDouble(expected: Double, angle: Double) {
        val actual = angle.toDegrees()
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun toDegreesFloat() {
        provideRadians().forEach { args -> toDegreesFloat(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun toDegreesFloat(expected: Double, angle: Double) {
        val actual = angle.toFloat().toDegrees()
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    @Test
    fun roundPlacesDouble() {
        provideRoundPlaces().forEach { args -> roundPlacesDouble(args[0].arg<Double>(), args[1].arg<Int>(), args[2].arg<Double>()) }
    }

    private fun roundPlacesDouble(value: Double, places: Int, expected: Double) {
        val actual = value.roundPlaces(places)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun roundPlacesFloat() {
        provideRoundPlaces().forEach { args -> roundPlacesFloat(args[0].arg<Double>(), args[1].arg<Int>(), args[2].arg<Double>()) }
    }

    private fun roundPlacesFloat(value: Double, places: Int, expected: Double) {
        val actual = value.toFloat().roundPlaces(places)
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    @Test
    fun positive() {
        positive(0.0f, 1.0f, 1.0f)
        positive(1.0f, 0.0f, 1.0f)
        positive(-1.0f, 0.0f, 1.0f)
        positive(Float.NaN, 1.0f, Float.NaN)
        positive(Float.POSITIVE_INFINITY, 1.0f, Float.POSITIVE_INFINITY)
        positive(Float.NEGATIVE_INFINITY, 1.0f, Float.POSITIVE_INFINITY)
    }

    private fun positive(value: Float, zeroReplacement: Float, expected: Float) {
        val actual = value.positive(zeroReplacement)
        assertEquals(expected, actual)
    }

    @Test
    fun negative() {
        negative(0.0f, -1.0f, -1.0f)
        negative(1.0f, 0.0f, -1.0f)
        negative(-1.0f, 0.0f, -1.0f)
        negative(Float.NaN, -1.0f, Float.NaN)
        negative(Float.POSITIVE_INFINITY, -1.0f, Float.NEGATIVE_INFINITY)
        negative(Float.NEGATIVE_INFINITY, -1.0f, Float.NEGATIVE_INFINITY)
    }

    private fun negative(value: Float, zeroReplacement: Float, expected: Float) {
        val actual = value.negative(zeroReplacement)
        assertEquals(expected, actual)
    }

    @Test
    fun real() {
        real(Float.NaN, 1.0f, 1.0f)
        real(Float.POSITIVE_INFINITY, 1.0f, 1.0f)
        real(Float.NEGATIVE_INFINITY, 1.0f, 1.0f)
        real(0.0f, 1.0f, 0.0f)
        real(1.0f, 0.0f, 1.0f)
        real(-1.0f, 0.0f, -1.0f)
    }

    private fun real(value: Float, defaultValue: Float, expected: Float) {
        val actual = value.real(defaultValue)
        assertEquals(expected, actual)
    }

    @Test
    fun roundNearestDouble() {
        roundNearestDouble(1.0, 1.0, 1.0)
        roundNearestDouble(1.5, 1.0, 2.0)
        roundNearestDouble(1.4, 1.0, 1.0)
        roundNearestDouble(20.0, 10.0, 20.0)
        roundNearestDouble(20.0, 15.0, 15.0)
        roundNearestDouble(25.0, 15.0, 30.0)
        roundNearestDouble(0.0, 1.0, 0.0)
        roundNearestDouble(-1.0, 1.0, -1.0)
        roundNearestDouble(-1.5, 1.0, -1.0)
        roundNearestDouble(-1.6, 1.0, -2.0)
    }

    private fun roundNearestDouble(value: Double, nearest: Double, expected: Double) {
        val actual = value.roundNearest(nearest)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun roundNearestFloat() {
        roundNearestFloat(1.0f, 1.0f, 1.0f)
        roundNearestFloat(1.5f, 1.0f, 2.0f)
        roundNearestFloat(1.4f, 1.0f, 1.0f)
        roundNearestFloat(20.0f, 10.0f, 20.0f)
        roundNearestFloat(20.0f, 15.0f, 15.0f)
        roundNearestFloat(25.0f, 15.0f, 30.0f)
        roundNearestFloat(0.0f, 1.0f, 0.0f)
        roundNearestFloat(-1.0f, 1.0f, -1.0f)
        roundNearestFloat(-1.5f, 1.0f, -1.0f)
        roundNearestFloat(-1.6f, 1.0f, -2.0f)
    }

    private fun roundNearestFloat(value: Float, nearest: Float, expected: Float) {
        val actual = value.roundNearest(nearest)
        assertEquals(expected, actual, 0.00001f)
    }

    @Test
    fun roundNearestInt() {
        roundNearestInt(1, 1, 1)
        roundNearestInt(2, 1, 2)
        roundNearestInt(0, 2, 0)
        roundNearestInt(3, 2, 4)
        roundNearestInt(20, 10, 20)
        roundNearestInt(20, 16, 16)
        roundNearestInt(25, 16, 32)
        roundNearestInt(-1, 1, -1)
        roundNearestInt(-2, 5, 0)
        roundNearestInt(-3, 5, -5)
    }

    private fun roundNearestInt(value: Int, nearest: Int, expected: Int) {
        val actual = value.roundNearest(nearest)
        assertEquals(expected, actual)
    }

    @Test
    fun roundNearestAngleFloat() {
        roundNearestAngleFloat(0.0f, 15.0f, 0.0f)
        roundNearestAngleFloat(355.0f, 15.0f, 0.0f)
        roundNearestAngleFloat(5.0f, 15.0f, 0.0f)
        roundNearestAngleFloat(10.0f, 15.0f, 15.0f)
        roundNearestAngleFloat(20.0f, 15.0f, 15.0f)
        roundNearestAngleFloat(25.0f, 15.0f, 30.0f)
        roundNearestAngleFloat(-15.0f, 15.0f, 345.0f)
        roundNearestAngleFloat(-20.0f, 15.0f, 345.0f)
        roundNearestAngleFloat(-25.0f, 15.0f, 330.0f)
    }

    private fun roundNearestAngleFloat(value: Float, nearest: Float, expected: Float) {
        val actual = value.roundNearestAngle(nearest)
        assertEquals(expected, actual, 0.00001f)
    }

    @Test
    fun roundNearestAngleDouble() {
        roundNearestAngleDouble(0.0, 15.0, 0.0)
        roundNearestAngleDouble(355.0, 15.0, 0.0)
        roundNearestAngleDouble(5.0, 15.0, 0.0)
        roundNearestAngleDouble(10.0, 15.0, 15.0)
        roundNearestAngleDouble(20.0, 15.0, 15.0)
        roundNearestAngleDouble(25.0, 15.0, 30.0)
        roundNearestAngleDouble(-15.0, 15.0, 345.0)
        roundNearestAngleDouble(-20.0, 15.0, 345.0)
        roundNearestAngleDouble(-25.0, 15.0, 330.0)
    }

    private fun roundNearestAngleDouble(value: Double, nearest: Double, expected: Double) {
        val actual = value.roundNearestAngle(nearest)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun round() {
        round(RoundingMethod.AwayFromZero, 1.0f, 1)
        round(RoundingMethod.AwayFromZero, 1.1f, 1)
        round(RoundingMethod.AwayFromZero, 1.5f, 2)
        round(RoundingMethod.AwayFromZero, 1.9f, 2)
        round(RoundingMethod.AwayFromZero, -1.0f, -1)
        round(RoundingMethod.AwayFromZero, -1.1f, -1)
        round(RoundingMethod.AwayFromZero, -1.5f, -2)
        round(RoundingMethod.AwayFromZero, -1.9f, -2)
        round(RoundingMethod.TowardZero, 1.0f, 1)
        round(RoundingMethod.TowardZero, 1.1f, 1)
        round(RoundingMethod.TowardZero, 1.5f, 1)
        round(RoundingMethod.TowardZero, 1.9f, 2)
        round(RoundingMethod.TowardZero, -1.0f, -1)
        round(RoundingMethod.TowardZero, -1.1f, -1)
        round(RoundingMethod.TowardZero, -1.5f, -1)
        round(RoundingMethod.TowardZero, -1.9f, -2)
    }

    private fun round(method: RoundingMethod, value: Float, expected: Int) {
        val actual = value.round(method)
        assertEquals(expected, actual)
    }

    companion object {
        fun provideRadians(): List<List<Any?>> {
            return listOf(
                listOf(45.0, Math.toRadians(45.0)),
                listOf(90.0, Math.toRadians(90.0)),
                listOf(0.0, Math.toRadians(0.0)),
                listOf(-90.0, Math.toRadians(-90.0)),
                listOf(720.0, Math.toRadians(720.0)),
            )
        }
        fun provideRoundPlaces(): List<List<Any?>> {
            return listOf(
                // Floor
                listOf(1.1111, 0, 1.0),
                listOf(1.1111, 1, 1.1),
                listOf(1.1111, 2, 1.11),
                listOf(1.1111, 3, 1.111),
                listOf(1.1111, 4, 1.1111),
                listOf(1.1111, 5, 1.1111),

                // Ceil
                listOf(1.6666, 0, 2.0),
                listOf(1.6666, 1, 1.7),
                listOf(1.6666, 2, 1.67),
                listOf(1.6666, 3, 1.667),
                listOf(1.6666, 4, 1.6666),
                listOf(1.6666, 5, 1.6666),

                // Middle
                listOf(1.5555, 0, 2.0),
                listOf(1.5555, 1, 1.6),
                listOf(1.5555, 2, 1.56),
                listOf(1.5555, 3, 1.556),
                listOf(1.5555, 4, 1.5555),
                listOf(1.5555, 5, 1.5555),

                // Negative
                listOf(15.11, -1, 20.0),
                listOf(15.11, -2, 0.0),
                listOf(55.11, -2, 100.0),
                listOf(155.11, -2, 200.0),

                // Large
                listOf(8000000.0, 5, 8000000.0),
                listOf(8000000.125555, 5, 8000000.12556),
                listOf(8000000.125555555555, 8, 8000000.12555556),
            )
        }
    }
}
