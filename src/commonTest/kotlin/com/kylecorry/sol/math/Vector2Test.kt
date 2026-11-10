package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class Vector2Test {

    @Test
    fun rotate() {
        provideRotation().forEach { args -> rotate(args[0].arg<Vector2>(), args[1].arg<Float>(), args[2].arg<Vector2>(), args[3].arg<Vector2>()) }
    }

    private fun rotate(
        vector: Vector2,
        angle: Float,
        origin: Vector2,
        expected: Vector2
    ) {
        val rotated = vector.rotate(angle, origin)

        assertEquals(expected.x, rotated.x, 0.0001f)
        assertEquals(expected.y, rotated.y, 0.0001f)
    }

    // Rotate default (origin)
    @Test
    fun rotateDefault() {
        rotateDefault(1f, 0f, 90f, 0f, 1f)
        rotateDefault(1f, 0f, 180f, -1f, 0f)
        rotateDefault(1f, 0f, 270f, 0f, -1f)
        rotateDefault(1f, 0f, 360f, 1f, 0f)
    }

    private fun rotateDefault(x: Float, y: Float, angle: Float, expectedX: Float, expectedY: Float) {
        val vector = Vector2(x, y)
        val rotated = vector.rotate(angle)

        assertEquals(expectedX, rotated.x, 0.0001f)
        assertEquals(expectedY, rotated.y, 0.0001f)
    }

    @Test
    fun angle() {
        angle(0f, 0f, 0f)
        angle(1f, 0f, 0f)
        angle(0f, 1f, 90f)
        angle(-1f, 0f, 180f)
        angle(0f, -1f, 270f)
        angle(1f, 1f, 45f)
        angle(-1f, 1f, 135f)
        angle(-1f, -1f, 225f)
        angle(1f, -1f, 315f)
    }

    private fun angle(x: Float, y: Float, expected: Float) {
        val vector = Vector2(x, y)
        assertEquals(expected, vector.angle(), 0.0001f)
    }

    @Test
    fun plus() {
        plus(0f, 0f, 0f, 0f, 0f, 0f)
        plus(1f, 0f, 0f, 0f, 1f, 0f)
        plus(0f, 1f, 0f, 0f, 0f, 1f)
        plus(1f, 1f, 0f, 0f, 1f, 1f)
        plus(1f, 1f, 1f, 1f, 2f, 2f)
        plus(1f, 1f, -1f, -1f, 0f, 0f)
        plus(1f, 1f, 1f, -1f, 2f, 0f)
        plus(1f, 1f, -1f, 1f, 0f, 2f)
    }

    private fun plus(x: Float, y: Float, otherX: Float, otherY: Float, expectedX: Float, expectedY: Float) {
        val vector = Vector2(x, y)
        val other = Vector2(otherX, otherY)
        val result = vector + other
        assertEquals(expectedX, result.x, 0.0001f)
        assertEquals(expectedY, result.y, 0.0001f)
    }

    @Test
    fun minus() {
        minus(0f, 0f, 0f, 0f, 0f, 0f)
        minus(1f, 0f, 0f, 0f, 1f, 0f)
        minus(0f, 1f, 0f, 0f, 0f, 1f)
        minus(1f, 1f, 0f, 0f, 1f, 1f)
        minus(1f, 1f, 1f, 1f, 0f, 0f)
        minus(1f, 1f, -1f, -1f, 2f, 2f)
        minus(1f, 1f, 1f, -1f, 0f, 2f)
        minus(1f, 1f, -1f, 1f, 2f, 0f)
    }

    private fun minus(x: Float, y: Float, otherX: Float, otherY: Float, expectedX: Float, expectedY: Float) {
        val vector = Vector2(x, y)
        val other = Vector2(otherX, otherY)
        val result = vector - other
        assertEquals(expectedX, result.x, 0.0001f)
        assertEquals(expectedY, result.y, 0.0001f)
    }

    @Test
    fun times() {
        times(0f, 0f, 0f, 0f, 0f)
        times(1f, 0f, 0f, 0f, 0f)
        times(0f, 1f, 0f, 0f, 0f)
        times(1f, 0f, 1f, 1f, 0f)
        times(0f, 1f, 1f, 0f, 1f)
        times(1f, 2f, 3f, 3f, 6f)
        times(1f, 2f, -1f, -1f, -2f)
    }

    private fun times(x: Float, y: Float, scale: Float, expectedX: Float, expectedY: Float) {
        val vector = Vector2(x, y)
        val result = vector * scale
        assertEquals(expectedX, result.x, 0.0001f)
        assertEquals(expectedY, result.y, 0.0001f)
    }

    @Test
    fun magnitude() {
        magnitude(0f, 0f, 0f)
        magnitude(1f, 0f, 1f)
        magnitude(0f, 1f, 1f)
        magnitude(1f, 1f, 1.4142135f)
    }

    private fun magnitude(x: Float, y: Float, expected: Float) {
        val vector = Vector2(x, y)
        assertEquals(expected, vector.magnitude(), 0.0001f)
    }

    @Test
    fun distanceTo() {
        distanceTo(0f, 0f, 0f, 0f, 0f)
        distanceTo(1f, 0f, 0f, 0f, 1f)
        distanceTo(0f, 1f, 0f, 0f, 1f)
        distanceTo(1f, 1f, 0f, 0f, 1.4142135f)
        distanceTo(1f, 1f, 0f, 1f, 1f)
    }

    private fun distanceTo(x: Float, y: Float, otherX: Float, otherY: Float, expected: Float) {
        val vector = Vector2(x, y)
        val other = Vector2(otherX, otherY)
        assertEquals(expected, vector.distanceTo(other), 0.0001f)
    }

    // Normalize
    @Test
    fun normalize() {
        normalize(0f, 0f, 0f, 0f)
        normalize(1f, 0f, 1f, 0f)
        normalize(0f, 1f, 0f, 1f)
        normalize(1f, 1f, 0.70710677f, 0.70710677f)
        normalize(1f, 2f, 0.4472136f, 0.8944272f)
    }

    private fun normalize(x: Float, y: Float, expectedX: Float, expectedY: Float) {
        val vector = Vector2(x, y)
        val normalized = vector.normalize()
        assertEquals(expectedX, normalized.x, 0.0001f)
        assertEquals(expectedY, normalized.y, 0.0001f)
    }

    @Test
    fun squaredMagnitude() {
        squaredMagnitude(0f, 0f, 0f)
        squaredMagnitude(1f, 0f, 1f)
        squaredMagnitude(0f, 1f, 1f)
        squaredMagnitude(1f, 1f, 2f)
    }

    private fun squaredMagnitude(x: Float, y: Float, expected: Float) {
        val vector = Vector2(x, y)
        assertEquals(expected, vector.squaredMagnitude(), 0.0001f)
    }

    @Test
    fun squaredDistanceTo() {
        squaredDistanceTo(0f, 0f, 0f, 0f, 0f)
        squaredDistanceTo(1f, 0f, 0f, 0f, 1f)
        squaredDistanceTo(0f, 1f, 0f, 0f, 1f)
        squaredDistanceTo(1f, 1f, 0f, 0f, 2f)
        squaredDistanceTo(1f, 1f, 0f, 1f, 1f)
    }

    private fun squaredDistanceTo(x: Float, y: Float, otherX: Float, otherY: Float, expected: Float) {
        val vector = Vector2(x, y)
        val other = Vector2(otherX, otherY)
        assertEquals(expected, vector.squaredDistanceTo(other), 0.0001f)
    }

    // Angle between
    @Test
    fun angleBetween() {
        angleBetween(0f, 0f, 0f, 0f, 0f)
        angleBetween(1f, 0f, 0f, 0f, 180f)
        angleBetween(0f, 1f, 0f, 0f, 270f)
        angleBetween(1f, 1f, 0f, 0f, 225f)
        angleBetween(1f, 1f, 1f, 1f, 0f)
        angleBetween(1f, 1f, -1f, -1f, 225f)
        angleBetween(1f, 1f, 1f, -1f, 270f)
        angleBetween(1f, 1f, -1f, 1f, 180f)
    }

    private fun angleBetween(x: Float, y: Float, otherX: Float, otherY: Float, expected: Float) {
        val vector = Vector2(x, y)
        val other = Vector2(otherX, otherY)
        assertEquals(expected, vector.angleBetween(other), 0.0001f)
    }

    @Test
    fun coerceIn() {
        coerceIn(0f, 0f, -1f, -1f, 1f, 1f, 0f, 0f)
        coerceIn(-2f, 0f, -1f, -1f, 1f, 1f, -1f, 0f)
        coerceIn(0f, 2f, -1f, -1f, 1f, 1f, 0f, 1f)
        coerceIn(-2f, 2f, -1f, -1f, 1f, 1f, -1f, 1f)
        coerceIn(2f, -2f, -1f, -1f, 1f, 1f, 1f, -1f)
    }

    private fun coerceIn(
        x: Float,
        y: Float,
        minimumX: Float,
        minimumY: Float,
        maximumX: Float,
        maximumY: Float,
        expectedX: Float,
        expectedY: Float
    ) {
        val vector = Vector2(x, y)
        val minimum = Vector2(minimumX, minimumY)
        val maximum = Vector2(maximumX, maximumY)

        val result = vector.coerceIn(minimum, maximum)

        assertEquals(expectedX, result.x, 0.0001f)
        assertEquals(expectedY, result.y, 0.0001f)
    }

    companion object {
        fun provideRotation(): List<List<Any?>> {
            return listOf(
                // CW
                listOf(Vector2(1f, 0f), 90f, Vector2.zero, Vector2(0f, 1f)),
                listOf(Vector2(1f, 0f), 180f, Vector2.zero, Vector2(-1f, 0f)),
                listOf(Vector2(1f, 0f), 270f, Vector2.zero, Vector2(0f, -1f)),
                listOf(Vector2(1f, 0f), 360f, Vector2.zero, Vector2(1f, 0f)),
                listOf(Vector2(2f, 0f), 90f, Vector2(1f, 2f), Vector2(3f, 3f)),
                listOf(Vector2(2f, 0f), 180f, Vector2(1f, 2f), Vector2(0f, 4f)),
                listOf(Vector2(2f, 0f), 270f, Vector2(1f, 2f), Vector2(-1f, 1f)),
                listOf(Vector2(2f, 0f), 360f, Vector2(1f, 2f), Vector2(2f, 0f)),
                // CCW
                listOf(Vector2(1f, 0f), -90f, Vector2.zero, Vector2(0f, -1f)),
                listOf(Vector2(1f, 0f), -180f, Vector2.zero, Vector2(-1f, 0f)),
                listOf(Vector2(1f, 0f), -270f, Vector2.zero, Vector2(0f, 1f)),
                listOf(Vector2(1f, 0f), -360f, Vector2.zero, Vector2(1f, 0f)),
                listOf(Vector2(2f, 0f), -90f, Vector2(1f, 2f), Vector2(-1f, 1f)),
                listOf(Vector2(2f, 0f), -180f, Vector2(1f, 2f), Vector2(0f, 4f)),
                listOf(Vector2(2f, 0f), -270f, Vector2(1f, 2f), Vector2(3f, 3f)),
                listOf(Vector2(2f, 0f), -360f, Vector2(1f, 2f), Vector2(2f, 0f)),
                // No rotation
                listOf(Vector2(1f, 0f), 0f, Vector2.zero, Vector2(1f, 0f)),
                listOf(Vector2(1f, 0f), 0f, Vector2(1f, 0f), Vector2(1f, 0f)),
            )
        }
    }

}
