package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals

class SphericalCoordinateTest {

    @Test
    fun toCartesian() {
        toCartesian(1f, 45f, 45f, 0.5f, 0.5f, 0.707f)
        toCartesian(1f, 0f, 0f, 0f, 0f, 1f)
        toCartesian(1f, 90f, 0f, 1f, 0f, 0f)
        toCartesian(1f, 0f, 90f, 0f, 0f, 1f)
        toCartesian(1f, 180f, 0f, 0f, 0f, -1f)
        toCartesian(1f, 0f, 180f, 0f, 0f, 1f)
        toCartesian(1f, 270f, 0f, -1f, 0f, 0f)
        toCartesian(1f, 0f, 270f, 0f, 0f, 1f)
        toCartesian(15f, 20f, 10f, 5.05f, 0.89f, 14.1f)
        toCartesian(0f, 20f, 10f, 0f, 0f, 0f)
    }

    private fun toCartesian(r: Float, theta: Float, phi: Float, x: Float, y: Float, z: Float) {
        val spherical = SphericalCoordinate(r, theta, phi)
        val cartesian = spherical.toCartesian()
        approxEquals(Vector3(x, y, z), cartesian, 0.01f)
    }

    @Test
    fun fromCartesian() {
        fromCartesian(0.5f, 0.5f, 0.707f, 1f, 45f, 45f)
        fromCartesian(0f, 0f, 1f, 1f, 0f, 0f)
        fromCartesian(1f, 0f, 0f, 1f, 90f, 0f)
        fromCartesian(0f, 0f, 1f, 1f, 0f, 0f)
        fromCartesian(0f, 0f, -1f, 1f, 180f, 0f)
        //        "-1, 0, 0, 1, 0, 0",
        fromCartesian(5.05f, 0.89f, 14.1f, 15f, 20f, 10f)
        fromCartesian(0f, 0f, 0f, 0f, 0f, 0f)
    }

    private fun fromCartesian(x: Float, y: Float, z: Float, r: Float, theta: Float, phi: Float) {
        val cartesian = Vector3(x, y, z)
        val spherical = SphericalCoordinate.fromCartesian(cartesian)
        approxEquals(SphericalCoordinate(r, theta, phi), spherical, 0.1f)
    }

    private fun approxEquals(expected: Vector3, actual: Vector3, tolerance: Float) {
        assertEquals(expected.x, actual.x, tolerance)
        assertEquals(expected.y, actual.y, tolerance)
        assertEquals(expected.z, actual.z, tolerance)
    }

    private fun approxEquals(expected: SphericalCoordinate, actual: SphericalCoordinate, tolerance: Float) {
        assertEquals(expected.r, actual.r, tolerance)
        assertEquals(expected.theta, actual.theta, tolerance)
        assertEquals(expected.phi, actual.phi, tolerance)
    }
}