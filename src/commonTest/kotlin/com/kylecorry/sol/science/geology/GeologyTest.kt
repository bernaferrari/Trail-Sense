package com.kylecorry.sol.science.geology

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.units.*

internal class GeologyTest {

    @Test
    fun getAvalancheRisk() {
        provideAvalancheRisk().forEach { args -> getAvalancheRisk(args[0].arg<Float>(), args[1].arg<AvalancheRisk>()) }
    }

    private fun getAvalancheRisk(angle: Float, expected: AvalancheRisk) {
        val risk = Geology.getAvalancheRisk(angle)
        assertEquals(expected, risk)
    }

    @Test
    fun calculateHeights() {
        provideHeights().forEach { args -> calculateHeights(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>(), args[3].arg<Float>()) }
    }

    private fun calculateHeights(distance: Float, upAngle: Float, downAngle: Float, expected: Float) {
        val height = Geology.getHeightFromInclination(Distance.meters(distance), downAngle, upAngle)
        assertEquals(expected, height.value, 0.01f)
    }

    @Test
    fun calculateDistance() {
        provideDistances().forEach { args -> calculateDistance(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>(), args[3].arg<Float>()) }
    }

    private fun calculateDistance(height: Float, upAngle: Float, downAngle: Float, expected: Float) {
        val d = Geology.getDistanceFromInclination(Distance.meters(height), downAngle, upAngle)
        assertEquals(expected, d.value, 0.01f)
    }

    @Test
    fun getInclination() {
        provideInclination().forEach { args -> getInclination(args[0].arg<Float>(), args[1].arg<Float>()) }
    }

    private fun getInclination(angle: Float, expected: Float) {
        val inclination = Geology.getInclination(angle)
        assertEquals(expected, inclination, 0.01f)
    }

    @Test
    fun getSlopeGrade() {
        provideGrade().forEach { args -> getSlopeGrade(args[0].arg<Float>(), args[1].arg<Float>()) }
    }

    private fun getSlopeGrade(angle: Float, expected: Float) {
        val grade = Geology.getSlopeGrade(angle)
        assertEquals(expected, grade, 0.01f)
    }

    @Test
    fun getSlopeGradeFromDistance() {
        provideGradeDistance().forEach { args -> getSlopeGradeFromDistance(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun getSlopeGradeFromDistance(verticalMeters: Float, horizontalMeters: Float, expected: Float) {
        val vertical = Distance.meters(verticalMeters)
        val horizontal = Distance.meters(horizontalMeters)
        val grade = Geology.getSlopeGrade(vertical, horizontal)
        assertEquals(expected, grade, 0.01f)
    }

    @Test
    fun getElevationCurvatureCorrection() {
        getElevationCurvatureCorrection(0f, true, 0f)
        getElevationCurvatureCorrection(1000f, true, 0.067267f)
        getElevationCurvatureCorrection(1000f, false, 0.0784782f)
        getElevationCurvatureCorrection(10000f, true, 6.7267f)
        getElevationCurvatureCorrection(10000f, false, 7.84782f)
    }

    private fun getElevationCurvatureCorrection(distanceMeters: Float, withRefraction: Boolean, expected: Float) {
        val correction = Geology.getElevationCurvatureCorrection(
            Distance.meters(distanceMeters),
            withRefraction
        )
        assertEquals(expected, correction.value, 0.0001f)
    }

    @Test
    fun getHorizonDipAngle() {
        getHorizonDipAngle(80f, true, 1.42824f)
        getHorizonDipAngle(100f, true, 0.282622f)
        getHorizonDipAngle(125f, true, -1.01028f)
        getHorizonDipAngle(130f, false, -1.15475f)
    }

    private fun getHorizonDipAngle(
        currentElevationMeters: Float,
        withRefraction: Boolean,
        expected: Float
    ) {
        val angle = Geology.getHorizonDipAngle(
            Distance.meters(currentElevationMeters),
            horizonMeasurements,
            withRefraction
        )
        assertEquals(expected, angle, 0.0001f)
    }

    @Test
    fun getHorizonDipAngleRequiresMeasurements() {
        assertFailsWith<IllegalArgumentException> {
            Geology.getHorizonDipAngle(Distance.meters(100f), emptyList())
        }
    }

    companion object {

        private val horizonMeasurements = listOf(
            ElevationRayMeasurement(Distance.meters(1000f), Distance.meters(105f)),
            ElevationRayMeasurement(Distance.meters(2000f), Distance.meters(90f)),
            ElevationRayMeasurement(Distance.meters(3000f), Distance.meters(70f))
        )
        fun provideGrade(): List<List<Any?>> {
            return listOf(
                listOf(45f, 100f),
                listOf(25f, 46.63f),
                listOf(10f, 17.63f),
                listOf(0f, 0f),
                listOf(-45f, -100f),
                listOf(-25f, -46.63f),
                listOf(-10f, -17.63f),
                listOf(50f, 119.18f),
            )
        }
        fun provideGradeDistance(): List<List<Any?>> {
            return listOf(
                listOf(1f, 1f, 100f),
                listOf(20f, 1f, 5f),
                listOf(20f, 1f, 5f),
                listOf(0f, 10f, Float.POSITIVE_INFINITY),
                listOf(0f, 0f, 0f),
                listOf(0f, -10f, Float.NEGATIVE_INFINITY),
                listOf(20f, -1f, -5f),
                listOf(20f, -1f, -5f),
                listOf(1f, -1f, -100f),
            )
        }
        fun provideInclination(): List<List<Any?>> {
            return listOf(
                listOf(10f, 10f),
                listOf(90f, 90f),
                listOf(95f, 85f),
                listOf(180f, 0f),
                listOf(185f, -5f),
                listOf(270f, -90f),
                listOf(280f, -80f),
            )
        }
        fun provideHeights(): List<List<Any?>> {
            return listOf(
                listOf(10f, 45f, 0f, 10f),
                listOf(10f, 20f, 0f, 3.6397f),

                listOf(10f, 45f, -5f, 10.8748f),
                listOf(10f, 20f, 5f, 2.764814f),

                listOf(10f, -5f, -20f, 2.764814f),

                listOf(10f, 5f, 20f, 2.7648158f),
                listOf(10f, 90f, 0f, Float.POSITIVE_INFINITY),
                listOf(10f, 0f, 90f, Float.POSITIVE_INFINITY),
                listOf(10f, -90f, 0f, Float.POSITIVE_INFINITY),
                listOf(10f, 0f, -90f, Float.POSITIVE_INFINITY),
            )
        }
        fun provideDistances(): List<List<Any?>> {
            return listOf(
                listOf(15f, 0f, 4.5739f, 187.5f),
                listOf(10f, 45f, 0f, 10f),
                listOf(3.6397f, 20f, 0f, 10f),

                listOf(10.8748f, 45f, -5f, 10f),
                listOf(2.764814f, 20f, 5f, 10f),

                listOf(2.764814f, -5f, -20f, 10f),

                listOf(2.7648158f, 5f, 20f, 10f),
                listOf(10f, 90f, 0f, 0f),
                listOf(10f, 0f, 90f, 0f),
                listOf(10f, -90f, 0f, 0f),
                listOf(10f, 0f, -90f, 0f),
            )
        }
        fun provideAvalancheRisk(): List<List<Any?>> {
            return listOf(
                listOf(0f, AvalancheRisk.Low),
                listOf(19f, AvalancheRisk.Low),
                listOf(-19f, AvalancheRisk.Low),
                listOf(20f, AvalancheRisk.Low),
                listOf(29f, AvalancheRisk.Low),
                listOf(51f, AvalancheRisk.Moderate),
                listOf(90f, AvalancheRisk.Low),
                listOf(-51f, AvalancheRisk.Moderate),
                listOf(30f, AvalancheRisk.High),
                listOf(45f, AvalancheRisk.High),
                listOf(50f, AvalancheRisk.Moderate),
                listOf(-45f, AvalancheRisk.High),
            )
        }
    }
}