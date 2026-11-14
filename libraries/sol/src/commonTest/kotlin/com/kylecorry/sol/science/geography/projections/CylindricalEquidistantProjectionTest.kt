package com.kylecorry.sol.science.geography.projections

import kotlin.test.Test
import com.kylecorry.sol.tests.arg

import assertk.assertThat
import assertk.assertions.isCloseTo
import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.tests.isCloseTo

internal class CylindricalEquidistantProjectionTest {

    @Test
    fun toCoordinate() {
        provideToCoordinate().forEach { args -> toCoordinate(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Coordinate>()) }
    }

    private fun toCoordinate(x: Float, y: Float, expected: Coordinate) {
        val projection = CylindricalEquidistantProjection()
        val coordinate = projection.toCoordinate(Vector2(x, y))
        assertThat(coordinate).isCloseTo(expected, 0.5f)
    }

    @Test
    fun toPixels() {
        provideToPixels().forEach { args -> toPixels(args[0].arg<Coordinate>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun toPixels(coordinate: Coordinate, expectedX: Float, expectedY: Float) {
        val projection = CylindricalEquidistantProjection()
        val pixels = projection.toPixels(coordinate)
        assertThat(pixels.x).isCloseTo(expectedX, 0.5f)
        assertThat(pixels.y).isCloseTo(expectedY, 0.5f)
    }

    @Test
    fun getScaleForLatitude() {
        getScaleForLatitude(0.0, 1f)
        getScaleForLatitude(90.0, 0f)
        getScaleForLatitude(45.0, 0.7071068f)
        getScaleForLatitude(-45.0, 0.7071068f)
        getScaleForLatitude(30.0, 0.8660254f)
        getScaleForLatitude(-30.0, 0.8660254f)
        getScaleForLatitude(60.0, 0.5f)
        getScaleForLatitude(-60.0, 0.5f)
        getScaleForLatitude(80.0, 0.1736482f)
        getScaleForLatitude(-80.0, 0.1736482f)
    }

    private fun getScaleForLatitude(latitude: Double, expected: Float) {
        val scale = CylindricalEquidistantProjection.getScaleForLatitude(latitude)
        assertThat(scale).isCloseTo(expected, 0.0001f)
    }

    @Test
    fun toCoordinateScaled() {
        provideScaledToCoordinate().forEach { args -> toCoordinateScaled(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>(), args[3].arg<Coordinate>()) }
    }

    private fun toCoordinateScaled(scale: Float, x: Float, y: Float, expected: Coordinate) {
        val projection = CylindricalEquidistantProjection(scale)
        val coordinate = projection.toCoordinate(Vector2(x, y))
        assertThat(coordinate).isCloseTo(expected, 0.5f)
    }

    @Test
    fun toPixelsScaled() {
        provideScaledToPixels().forEach { args -> toPixelsScaled(args[0].arg<Float>(), args[1].arg<Coordinate>(), args[2].arg<Float>(), args[3].arg<Float>()) }
    }

    private fun toPixelsScaled(scale: Float, coordinate: Coordinate, expectedX: Float, expectedY: Float) {
        val projection = CylindricalEquidistantProjection(scale)
        val pixels = projection.toPixels(coordinate)
        assertThat(pixels.x).isCloseTo(expectedX, 0.0001f)
        assertThat(pixels.y).isCloseTo(expectedY, 0.0001f)
    }

    companion object {
        fun provideToCoordinate(): List<List<Any?>> {
            return listOf(
                listOf(0f, 0f, Coordinate.zero),
                listOf(10f.toRadians(), 0f, Coordinate(0.0, 10.0)),
                listOf((-10f).toRadians(), 0f, Coordinate(0.0, -10.0)),
                listOf(0f, (-10f).toRadians(), Coordinate(-10.0, 0.0)),
                listOf(0f, 10f.toRadians(), Coordinate(10.0, 0.0)),
                listOf(10f.toRadians(), 10f.toRadians(), Coordinate(10.0, 10.0)),
                listOf((-10f).toRadians(), (-10f).toRadians(), Coordinate(-10.0, -10.0)),
                listOf(200f.toRadians(), 0f, Coordinate(0.0, -160.0)),
                listOf((-200f).toRadians(), 0f, Coordinate(0.0, 160.0)),
                listOf(0f, 100f.toRadians(), Coordinate(90.0, 0.0)),
                listOf(0f, (-100f).toRadians(), Coordinate(-90.0, 0.0)),
            )
        }
        fun provideToPixels(): List<List<Any?>> {
            return listOf(
                listOf(Coordinate.zero, 0f, 0f),
                listOf(Coordinate(0.0, 10.0), 10f.toRadians(), 0f),
                listOf(Coordinate(0.0, -10.0), (-10f).toRadians(), 0f),
                listOf(Coordinate(-10.0, 0.0), 0f, (-10f).toRadians()),
                listOf(Coordinate(10.0, 0.0), 0f, 10f.toRadians()),
                listOf(Coordinate(10.0, 10.0), 10f.toRadians(), 10f.toRadians()),
                listOf(Coordinate(-10.0, -10.0), (-10f).toRadians(), (-10f).toRadians()),
                listOf(Coordinate(0.0, -160.0), (-160f).toRadians(), 0f),
                listOf(Coordinate(0.0, 160.0), 160f.toRadians(), 0f),
                listOf(Coordinate(90.0, 0.0), 0f, 100f.toRadians()),
                listOf(Coordinate(-90.0, 0.0), 0f, (-100f).toRadians()),
            )
        }
        fun provideScaledToCoordinate(): List<List<Any?>> {
            return listOf(
                listOf(2f, 20f.toRadians(), 20f.toRadians(), Coordinate(10.0, 10.0)),
                listOf(2f, (-20f).toRadians(), (-20f).toRadians(), Coordinate(-10.0, -10.0)),
            )
        }
        fun provideScaledToPixels(): List<List<Any?>> {
            return listOf(
                listOf(2f, Coordinate(10.0, 10.0), 20f.toRadians(), 20f.toRadians()),
                listOf(2f, Coordinate(-10.0, -10.0), (-20f).toRadians(), (-20f).toRadians()),
            )
        }
    }

}
