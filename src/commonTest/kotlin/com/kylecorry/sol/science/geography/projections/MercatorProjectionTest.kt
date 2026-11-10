package com.kylecorry.sol.science.geography.projections

import kotlin.test.Test
import com.kylecorry.sol.tests.arg

import assertk.assertThat
import assertk.assertions.isCloseTo
import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.tests.isCloseTo
import com.kylecorry.sol.units.Coordinate
import kotlin.math.PI

internal class MercatorProjectionTest {

    @Test
    fun toCoordinate() {
        provideToCoordinate().forEach { args -> toCoordinate(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Coordinate>()) }
    }

    private fun toCoordinate(x: Float, y: Float, expected: Coordinate) {
        val projection = MercatorProjection()
        val coordinate = projection.toCoordinate(Vector2(x, y))
        assertThat(coordinate).isCloseTo(expected, 0.6f)
    }

    @Test
    fun toPixels() {
        provideToPixels().forEach { args -> toPixels(args[0].arg<Coordinate>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun toPixels(coordinate: Coordinate, expectedX: Float, expectedY: Float) {
        val projection = MercatorProjection()
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
        val scale = MercatorProjection.getScaleForLatitude(latitude)
        assertThat(scale).isCloseTo(expected, 0.0001f)
    }

    @Test
    fun toCoordinateScaled() {
        provideScaledToCoordinate().forEach { args -> toCoordinateScaled(args[0].arg<Float>(), args[1].arg<Float>(), args[2].arg<Float>(), args[3].arg<Coordinate>()) }
    }

    private fun toCoordinateScaled(scale: Float, x: Float, y: Float, expected: Coordinate) {
        val projection = MercatorProjection(scale)
        val coordinate = projection.toCoordinate(Vector2(x, y))
        assertThat(coordinate).isCloseTo(expected, 0.5f)
    }

    @Test
    fun toPixelsScaled() {
        provideScaledToPixels().forEach { args -> toPixelsScaled(args[0].arg<Float>(), args[1].arg<Coordinate>(), args[2].arg<Float>(), args[3].arg<Float>()) }
    }

    private fun toPixelsScaled(scale: Float, coordinate: Coordinate, expectedX: Float, expectedY: Float) {
        val projection = MercatorProjection(scale)
        val pixels = projection.toPixels(coordinate)
        assertThat(pixels.x).isCloseTo(expectedX, 0.0001f)
        assertThat(pixels.y).isCloseTo(expectedY, 0.0001f)
    }

    companion object {
        fun provideToCoordinate(): List<List<Any?>> {
            return listOf(
                listOf(0f, 0f, Coordinate.zero),
                listOf(0f, 0.17542583f, Coordinate(10.0, 0.0)),
                listOf(0f, -0.17542583f, Coordinate(-10.0, 0.0)),
                listOf(0f, 2.4362462f, Coordinate(80.0, 0.0)),
                listOf(0f, -2.4362462f, Coordinate(-80.0, 0.0)),
                listOf(10f.toRadians(), 0f, Coordinate(0.0, 10.0)),
                listOf((-10f).toRadians(), 0f, Coordinate(0.0, -10.0)),
                listOf(10f.toRadians(), 0.17542583f, Coordinate(10.0, 10.0)),
                listOf((-10f).toRadians(), -0.17542583f, Coordinate(-10.0, -10.0)),
                listOf(10f.toRadians(), 2.4362462f, Coordinate(80.0, 10.0)),
                listOf((-10f).toRadians(), -2.4362462f, Coordinate(-80.0, -10.0)),
                listOf(0f, 200f, Coordinate(90.0, 0.0)),
                listOf(0f, -200f, Coordinate(-90.0, 0.0)),
                listOf(0f, PI.toFloat(), Coordinate(85.051129, 0.0)),
                listOf(0f, -PI.toFloat(), Coordinate(-85.051129, 0.0)),
                listOf(200f.toRadians(), 0f, Coordinate(0.0, -160.0)),
                listOf((-200f).toRadians(), 0f, Coordinate(0.0, 160.0)),
            )
        }
        fun provideToPixels(): List<List<Any?>> {
            return listOf(
                listOf(Coordinate.zero, 0f, 0f),
                listOf(Coordinate(10.0, 0.0), 0f, 0.17542583f),
                listOf(Coordinate(-10.0, 0.0), 0f, -0.17542583f),
                listOf(Coordinate(80.0, 0.0), 0f, 2.4362462f),
                listOf(Coordinate(-80.0, 0.0), 0f, -2.4362462f),
                listOf(Coordinate(0.0, 10.0), 10f.toRadians(), 0f),
                listOf(Coordinate(0.0, -10.0), (-10f).toRadians(), 0f),
                listOf(Coordinate(10.0, 10.0), 10f.toRadians(), 0.17542583f),
                listOf(Coordinate(-10.0, -10.0), (-10f).toRadians(), -0.17542583f),
                listOf(Coordinate(80.0, 10.0), 10f.toRadians(), 2.4362462f),
                listOf(Coordinate(-80.0, -10.0), (-10f).toRadians(), -2.4362462f),
                listOf(Coordinate(90.0, 0.0), 0f, Float.POSITIVE_INFINITY),
                listOf(Coordinate(-90.0, 0.0), 0f, Float.NEGATIVE_INFINITY),
                listOf(Coordinate(-85.051129, 0.0), 0f, -PI.toFloat()),
                listOf(Coordinate(85.051129, 0.0), 0f, PI.toFloat()),
                listOf(Coordinate(0.0, 160.0), 160f.toRadians(), 0f),
                listOf(Coordinate(0.0, -160.0), (-160f).toRadians(), 0f),
            )
        }
        fun provideScaledToCoordinate(): List<List<Any?>> {
            return listOf(
                listOf(2f, 20f.toRadians(), 0.35085166f, Coordinate(10.0, 10.0)),
                listOf(2f, (-20f).toRadians(), -0.35085166f, Coordinate(-10.0, -10.0)),
            )
        }
        fun provideScaledToPixels(): List<List<Any?>> {
            return listOf(
                listOf(2f, Coordinate(10.0, 10.0), 20f.toRadians(), 0.35085166f),
                listOf(2f, Coordinate(-10.0, -10.0), (-20f).toRadians(), -0.35085166f),
            )
        }
    }

}