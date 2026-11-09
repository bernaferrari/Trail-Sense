package com.kylecorry.sol.math.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.trigonometry.CosineWave
import com.kylecorry.sol.math.trigonometry.Trigonometry
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

internal class TrigonometryTest {

    @Test
    fun remapUnitAngle() {
        remapUnitAngle(0f, 0f, true, 0f)
        remapUnitAngle(0f, 0f, false, 0f)
        remapUnitAngle(0f, 90f, true, 270f)
        remapUnitAngle(0f, 90f, false, 90f)
        remapUnitAngle(90f, 0f, true, 90f)
        remapUnitAngle(90f, 0f, false, 270f)
        remapUnitAngle(90f, 90f, true, 0f)
        remapUnitAngle(90f, 90f, false, 0f)
        remapUnitAngle(180f, 0f, true, 180f)
        remapUnitAngle(180f, 0f, false, 180f)
        remapUnitAngle(180f, 90f, true, 90f)
        remapUnitAngle(180f, 90f, false, 270f)
        remapUnitAngle(270f, 0f, true, 270f)
        remapUnitAngle(270f, 0f, false, 90f)
        remapUnitAngle(270f, 90f, true, 180f)
        remapUnitAngle(270f, 90f, false, 180f)
        remapUnitAngle(90f, 360f, true, 90f)
        remapUnitAngle(90f, 360f, false, 270f)
    }

    private fun remapUnitAngle(
        originalAngle: Float,
        start: Float,
        isCounterClockwise: Boolean,
        expected: Float
    ) {
        val actual = Trigonometry.remapUnitAngle(originalAngle, start, isCounterClockwise)
        assertEquals(expected, actual, 0.0001f)
    }

    @Test
    fun toUnitAngle() {
        toUnitAngle(0f, 0f, true, 0f)
        toUnitAngle(0f, 0f, false, 0f)
        toUnitAngle(0f, 90f, true, 90f)
        toUnitAngle(0f, 90f, false, 90f)
        toUnitAngle(90f, 0f, true, 90f)
        toUnitAngle(90f, 0f, false, 270f)
        toUnitAngle(90f, 90f, true, 180f)
        toUnitAngle(90f, 90f, false, 0f)
        toUnitAngle(180f, 0f, true, 180f)
        toUnitAngle(180f, 0f, false, 180f)
        toUnitAngle(180f, 90f, true, 270f)
        toUnitAngle(180f, 90f, false, 270f)
        toUnitAngle(270f, 0f, true, 270f)
        toUnitAngle(270f, 0f, false, 90f)
        toUnitAngle(270f, 90f, true, 0f)
        toUnitAngle(270f, 90f, false, 180f)
        toUnitAngle(90f, 360f, true, 90f)
        toUnitAngle(90f, 360f, false, 270f)
        toUnitAngle(450f, 90f, true, 180f)
    }

    private fun toUnitAngle(
        originalAngle: Float,
        start: Float,
        isCounterClockwise: Boolean,
        expected: Float
    ) {
        val actual = Trigonometry.toUnitAngle(originalAngle, start, isCounterClockwise)
        assertEquals(expected, actual, 0.0001f)
    }

    @Test
    fun getRangeY() {
        getRangeY(0f, 0f, 0f, 0f)
        getRangeY(0f, 1f, 1f, 1f)
        getRangeY(1f, 0f, -1f, 1f)
        getRangeY(1f, 1f, 0f, 2f)
    }

    private fun getRangeY(amplitude: Float, verticalOffset: Float, expectedMin: Float, expectedMax: Float) {
        val wave = CosineWave(amplitude, 1f, 0f, verticalOffset)
        val range = Trigonometry.getRangeY(wave)
        assertEquals(expectedMin, range.start, 0.0001f)
        assertEquals(expectedMax, range.end, 0.0001f)
    }

    @Test
    fun getCombinationRangeY() {
        val waves = listOf(
            CosineWave(1f, 1f, 0f, 0f),
            CosineWave(2f, 3f, 1f, 1f),
            CosineWave(3f, 2f, 0f, 0f)
        )

        val range = Trigonometry.getCombinationRangeY(waves)

        assertEquals(-5f, range.start, 0.0001f)
        assertEquals(7f, range.end, 0.0001f)
    }

    @Test
    fun getAngularDistance() {
        // Identical points
        getAngularDistance(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        getAngularDistance(123.4f, 45.6f, 123.4f, 45.6f, 0.0f)
        // Along the horizon
        getAngularDistance(0.0f, 0.0f, 45.0f, 0.0f, 45.0f)
        getAngularDistance(0.0f, 0.0f, 90.0f, 0.0f, 90.0f)
        getAngularDistance(0.0f, 0.0f, 180.0f, 0.0f, 180.0f)
        // Along the same azimuth
        getAngularDistance(0.0f, 0.0f, 0.0f, 45.0f, 45.0f)
        getAngularDistance(0.0f, -45.0f, 0.0f, 0.0f, 45.0f)
        getAngularDistance(0.0f, 0.0f, 0.0f, -90.0f, 90.0f)
        // Zenith and nadir
        getAngularDistance(0.0f, 90.0f, 0.0f, -90.0f, 180.0f)
        getAngularDistance(0.0f, 90.0f, 45.0f, 0.0f, 90.0f)
        getAngularDistance(0.0f, 90.0f, 135.0f, 90.0f, 0.0f)
        // Across the 0/360 azimuth boundary
        getAngularDistance(359.0f, 0.0f, 1.0f, 0.0f, 2.0f)
        // Antipodal points
        getAngularDistance(10.0f, 45.0f, 190.0f, -45.0f, 180.0f)
        // Same altitude, different azimuth
        getAngularDistance(0.0f, 60.0f, 90.0f, 60.0f, 41.40962f)
        // Arbitrary points (and symmetry)
        getAngularDistance(30.0f, 60.0f, 100.0f, -30.0f, 106.55474f)
        getAngularDistance(100.0f, -30.0f, 30.0f, 60.0f, 106.55474f)
        // Small separation
        getAngularDistance(0.0f, 0.0f, 0.5f, 0.0f, 0.5f)
    }

    private fun getAngularDistance(
        azimuth1: Float,
        altitude1: Float,
        azimuth2: Float,
        altitude2: Float,
        expected: Float
    ) {
        val distance = Trigonometry.getAngularDistance(azimuth1, altitude1, azimuth2, altitude2)
        assertEquals(expected, distance, 0.05f)
    }

    @Test
    fun getAngularOffset() {
        getAngularOffset(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        getAngularOffset(0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f)
        getAngularOffset(0.0f, 0.0f, 359.0f, 0.0f, -1.0f, 0.0f)
        getAngularOffset(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f)
        getAngularOffset(0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f)
        getAngularOffset(359.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
        getAngularOffset(0.0f, 30.0f, 1.0f, 30.0f, 0.8660144f, 0.0037791f)
        getAngularOffset(30.0f, 45.0f, 31.0f, 46.0f, 0.6947468f, 1.0043609f)
        getAngularOffset(350.0f, 60.0f, 10.0f, 61.0f, 9.5533876f, 2.4858885f)
        getAngularOffset(45.0f, 30.0f, 50.0f, 32.0f, 4.2415596f, 2.0982498f)
    }

    private fun getAngularOffset(
        azimuth1: Float,
        altitude1: Float,
        azimuth2: Float,
        altitude2: Float,
        expectedEast: Float,
        expectedZenith: Float
    ) {
        val offset = Trigonometry.getAngularOffset(azimuth1, altitude1, azimuth2, altitude2)
        assertEquals(expectedEast, offset.x, 0.0001f)
        assertEquals(expectedZenith, offset.y, 0.0001f)
    }

    @Test
    fun normalizeAngle() {
        assertEquals(0.0, Trigonometry.normalizeAngle(0.0), 0.0)
        assertEquals(180.0, Trigonometry.normalizeAngle(180.0), 0.0)
        assertEquals(0.0, Trigonometry.normalizeAngle(0.0), 0.0)
        assertEquals(1.0, Trigonometry.normalizeAngle(361.0), 0.0)
        assertEquals(359.0, Trigonometry.normalizeAngle(-1.0), 0.0)
        assertEquals(180.0, Trigonometry.normalizeAngle(-180.0), 0.0)
        assertEquals(0.0, Trigonometry.normalizeAngle(720.0), 0.0)
    }

    @Test
    fun deltaAngle() {
        provideDeltaAngle().forEach { args -> deltaAngle(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun deltaAngle(angle1: Float, angle2: Float, expected: Float) {
        val actual = Trigonometry.deltaAngle(angle1, angle2)
        assertEquals(expected, actual, 0.00001f)
    }

    @Test
    fun tanDegreesDouble() {
        provideTanDegrees().forEach { args -> tanDegreesDouble(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun tanDegreesDouble(angle: Double, expected: Double) {
        val actual = Trigonometry.tanDegrees(angle)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun tanDegreesFloat() {
        provideTanDegrees().forEach { args -> tanDegreesFloat(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun tanDegreesFloat(angle: Double, expected: Double) {
        val actual = Trigonometry.tanDegrees(angle.toFloat())
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    @Test
    fun sinDegreesDouble() {
        provideSinDegrees().forEach { args -> sinDegreesDouble(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun sinDegreesDouble(angle: Double, expected: Double) {
        val actual = Trigonometry.sinDegrees(angle)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun sinDegreesFloat() {
        provideSinDegrees().forEach { args -> sinDegreesFloat(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun sinDegreesFloat(angle: Double, expected: Double) {
        val actual = Trigonometry.sinDegrees(angle.toFloat())
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    @Test
    fun cosDegreesDouble() {
        provideCosDegrees().forEach { args -> cosDegreesDouble(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun cosDegreesDouble(angle: Double, expected: Double) {
        val actual = Trigonometry.cosDegrees(angle)
        assertEquals(expected, actual, 0.00001)
    }

    @Test
    fun cosDegreesFloat() {
        provideCosDegrees().forEach { args -> cosDegreesFloat(args[0].arg<Double>(), args[1].arg<Double>()) }
    }

    private fun cosDegreesFloat(angle: Double, expected: Double) {
        val actual = Trigonometry.cosDegrees(angle.toFloat())
        assertEquals(expected.toFloat(), actual, 0.00001f)
    }

    companion object {
        fun provideDeltaAngle(): List<List<Any?>> {
            return listOf(
                listOf(0.0f, 0.0f, 0.0f),
                listOf(15.0f, 15.0f, 0.0f),
                listOf(15.0f, 25.0f, 10.0f),
                listOf(25.0f, 15.0f, -10.0f),
                listOf(0.0f, 360.0f, 0.0f),
                listOf(0.0f, 720.0f, 0.0f),
                listOf(10.0f, 370.0f, 0.0f),
                listOf(-10.0f, 370.0f, 20.0f),
                listOf(370.0f, -10.0f, -20.0f),
                listOf(10.0f, 180.0f, 170.0f),
                listOf(-10.0f, 180.0f, -170.0f),
                listOf(0.0f, 180.0f, 180.0f),
            )
        }
        fun provideTanDegrees(): List<List<Any?>> {
            return listOf(
                listOf(45.0, tan(Math.toRadians(45.0))),
                listOf(80.0, tan(Math.toRadians(80.0))),
                listOf(0.0, tan(Math.toRadians(0.0))),
                listOf(-80.0, tan(Math.toRadians(-80.0))),
                listOf(720.0, tan(Math.toRadians(720.0))),
            )
        }
        fun provideSinDegrees(): List<List<Any?>> {
            return listOf(
                listOf(45.0, sin(Math.toRadians(45.0))),
                listOf(90.0, sin(Math.toRadians(90.0))),
                listOf(0.0, sin(Math.toRadians(0.0))),
                listOf(-90.0, sin(Math.toRadians(-90.0))),
                listOf(720.0, sin(Math.toRadians(720.0))),
            )
        }
        fun provideCosDegrees(): List<List<Any?>> {
            return listOf(
                listOf(45.0, cos(Math.toRadians(45.0))),
                listOf(90.0, cos(Math.toRadians(90.0))),
                listOf(0.0, cos(Math.toRadians(0.0))),
                listOf(-90.0, cos(Math.toRadians(-90.0))),
                listOf(720.0, cos(Math.toRadians(720.0))),
            )
        }
    }
}
