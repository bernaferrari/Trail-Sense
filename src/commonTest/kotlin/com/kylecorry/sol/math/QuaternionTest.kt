package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import com.kylecorry.sol.tests.arg

class QuaternionTest {

    @Test
    fun toFloatArray() {
        val q = Quaternion(1f, 2f, 3f, 4f)
        assertContentEquals(floatArrayOf(1f, 2f, 3f, 4f), q.toFloatArray())
    }

    @Test
    fun times() {
        provideTimes().forEach { args -> times(args[0].arg<Quaternion>(), args[1].arg<Quaternion>(), args[2].arg<Quaternion>()) }
    }

    private fun times(a: Quaternion, b: Quaternion, expected: Quaternion) {
        approxEquals(expected, a * b, 0.001f)
    }

    @Test
    fun plus() {
        providePlus().forEach { args -> plus(args[0].arg<Quaternion>(), args[1].arg<Quaternion>(), args[2].arg<Quaternion>()) }
    }

    private fun plus(a: Quaternion, b: Quaternion, expected: Quaternion) {
        approxEquals(expected, a + b, 0.001f)
    }

    @Test
    fun minus() {
        provideMinus().forEach { args -> minus(args[0].arg<Quaternion>(), args[1].arg<Quaternion>(), args[2].arg<Quaternion>()) }
    }

    private fun minus(a: Quaternion, b: Quaternion, expected: Quaternion) {
        approxEquals(expected, a - b, 0.001f)
    }

    @Test
    fun normalize() {
        provideNormal().forEach { args -> normalize(args[0].arg<Quaternion>(), args[1].arg<Quaternion>()) }
    }

    private fun normalize(a: Quaternion, expected: Quaternion) {
        approxEquals(expected, a.normalize(), 0.001f)
    }

    @Test
    fun conjugate() {
        provideConjugate().forEach { args -> conjugate(args[0].arg<Quaternion>(), args[1].arg<Quaternion>()) }
    }

    private fun conjugate(a: Quaternion, expected: Quaternion) {
        approxEquals(expected, a.conjugate(), 0.001f)
    }

    @Test
    fun inverse() {
        provideInverse().forEach { args -> inverse(args[0].arg<Quaternion>(), args[1].arg<Quaternion>()) }
    }

    private fun inverse(a: Quaternion, expected: Quaternion) {
        approxEquals(expected, a.inverse(), 0.001f)
    }

    @Test
    fun rotate() {
        provideRotate().forEach { args -> rotate(args[0].arg<Quaternion>(), args[1].arg<Vector3>(), args[2].arg<Vector3>()) }
    }

    private fun rotate(a: Quaternion, v: Vector3, expected: Vector3) {
        approxEquals(expected, a.rotate(v), 0.001f)
    }

    @Test
    fun toEuler() {
        provideEuler().forEach { args -> toEuler(args[0].arg<Quaternion>(), args[1].arg<Euler>()) }
    }

    private fun toEuler(a: Quaternion, expected: Euler) {
        approxEquals(expected, a.toEuler(), 0.001f)
    }

    @Test
    fun fromEuler() {
        provideEuler().forEach { args -> fromEuler(args[0].arg<Quaternion>(), args[1].arg<Euler>()) }
    }

    private fun fromEuler(expected: Quaternion, a: Euler) {
        approxEquals(expected, Quaternion.from(a), 0.001f)
        approxEquals(expected, a.toQuaternion(), 0.001f)
    }

    @Test
    fun magnitude() {
        provideMagnitude().forEach { args -> magnitude(args[0].arg<Quaternion>(), args[1].arg<Float>()) }
    }

    private fun magnitude(a: Quaternion, expected: Float) {
        assertEquals(expected, a.magnitude(), 0.001f)
    }

    @Test
    fun slerp() {
        provideSlerp().forEach { args -> slerp(args[0].arg<Quaternion>(), args[1].arg<Quaternion>(), args[2].arg<Float>(), args[3].arg<Quaternion>()) }
    }

    private fun slerp(a: Quaternion, b: Quaternion, t: Float, expected: Quaternion) {
        approxEquals(expected, a.slerp(b, t), 0.001f)
    }

    @Test
    fun slerpNearlyIdentical() {
        slerpNearlyIdentical(0f, 0f)
        slerpNearlyIdentical(0.25f, 0.000125f)
        slerpNearlyIdentical(0.5f, 0.00025f)
        slerpNearlyIdentical(1f, 0.0005f)
    }

    private fun slerpNearlyIdentical(t: Float, expectedY: Float) {
        val a = Quaternion(0f, 0f, 0f, 1f)
        val b = Quaternion(0f, 0.0005f, 0f, 1f).normalize()

        approxEquals(Quaternion(0f, expectedY, 0f, 1f), a.slerp(b, t), 0.00001f)
    }

    @Test
    fun lerp() {
        provideLerp().forEach { args -> lerp(args[0].arg<Quaternion>(), args[1].arg<Quaternion>(), args[2].arg<Float>(), args[3].arg<Quaternion>()) }
    }

    private fun lerp(a: Quaternion, b: Quaternion, t: Float, expected: Quaternion) {
        approxEquals(expected, a.lerp(b, t), 0.001f)
    }

    @Test
    fun dot() {
        dot(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 70f)
        dot(-1f, -2f, -3f, -4f, -5f, -6f, -7f, -8f, 70f)
        dot(1f, 2f, 3f, 4f, -5f, -6f, -7f, -8f, -70f)
    }

    private fun dot(ax: Float, ay: Float, az: Float, aw: Float, bx: Float, by: Float, bz: Float, bw: Float, expected: Float) {
        val a = Quaternion(ax, ay, az, aw)
        val b = Quaternion(bx, by, bz, bw)
        assertEquals(expected, a.dot(b), 0.001f)
    }

    companion object {

        fun approxEquals(a: Quaternion, b: Quaternion, threshold: Float) {
            assertEquals(a.x, b.x, threshold)
            assertEquals(a.y, b.y, threshold)
            assertEquals(a.z, b.z, threshold)
            assertEquals(a.w, b.w, threshold)
        }

        fun approxEquals(a: Vector3, b: Vector3, threshold: Float) {
            assertEquals(a.x, b.x, threshold)
            assertEquals(a.y, b.y, threshold)
            assertEquals(a.z, b.z, threshold)
        }

        fun approxEquals(a: Euler, b: Euler, threshold: Float) {
            assertEquals(a.roll, b.roll, threshold)
            assertEquals(a.pitch, b.pitch, threshold)
            assertEquals(a.yaw, b.yaw, threshold)
        }
        fun provideTimes(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(6.25f, 0f, 1.25f, -2.5f)
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(-3.75f, 2f, 4.75f, -2.5f)
                )
            )
        }
        fun providePlus(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(2.25f, 2.5f, 5f, 1.5f)
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(2.25f, 2.5f, 5f, 1.5f)
                )
            )
        }
        fun provideMinus(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(1.75f, 3.5f, 3f, 0.5f)
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(-1.75f, -3.5f, -3f, -0.5f)
                )
            )
        }
        fun provideRotate(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Vector3(1f, 0f, 0f),
                    Vector3(0f, 0f, -1f)
                )
            )
        }
        fun provideMagnitude(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    5.477225575051661f
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    1.25f
                )
            )
        }
        fun provideSlerp(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.5f,
                    Quaternion(0.0f, 0.382f, 0.0f, 0.924f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.25f,
                    Quaternion(0.0f, 0.555f, 0.0f, 0.832f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.75f,
                    Quaternion(0.0f, 0.195f, 0.0f, 0.980f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.0f,
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    1.0f,
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                ),
                // Over 1
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    1.5f,
                    Quaternion(0.0f, -0.3826f, 0.0f, 0.9239f)
                ),
                // Under 0
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    -0.5f,
                    Quaternion(0.0f, 0.9237f, 0.0f, 0.3825f)
                ),
            )
        }
        fun provideLerp(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.5f,
                    Quaternion(0.0f, 0.382f, 0.0f, 0.924f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.25f,
                    Quaternion(0.0f, 0.5621f, 0.0f, 0.8271f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.75f,
                    Quaternion(0.0f, 0.1873f, 0.0f, 0.9823f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    0.0f,
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f)
                ),
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    1.0f,
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
                ),
                // Over 1
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    1.5f,
                    Quaternion(0.0f, -0.2946f, 0.0f, 0.9556f)
                ),
                // Under 0
                listOf(
                    Quaternion(0.0f, 0.707f, 0.0f, 0.707f),
                    Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                    -0.5f,
                    Quaternion(0.0f, 0.8841f, 0.0f, 0.4673f)
                ),
            )
        }
        fun provideNormal(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(
                        0.365148371670111f,
                        0.5477225f,
                        0.730296743340222f,
                        0.182574185835055f
                    )
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(0.2f, -0.4f, 0.8f, 0.4f)
                )
            )
        }
        fun provideConjugate(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(-2f, -3f, -4f, 1f)
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(-0.25f, 0.5f, -1f, 0.5f)
                )
            )
        }
        fun provideInverse(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f),
                    Quaternion(-0.0666667f, -0.1f, -0.1333333f, 0.0333333f)
                ),
                listOf(
                    Quaternion(0.25f, -0.5f, 1f, 0.5f),
                    Quaternion(-0.16f, 0.32f, -0.64f, 0.32f)
                )
            )
        }
        fun provideEuler(): List<List<Any?>> {
            return listOf(
                listOf(
                    Quaternion(2f, 3f, 4f, 1f).normalize(),
                    Euler(81.87f, -19.471f, 135f)
                ),
            )
        }

    }

}