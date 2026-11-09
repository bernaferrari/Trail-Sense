package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class BearingTest {

    @Test
    fun getDirection() {
        provideDirection().forEach { args -> getDirection(args[0].arg<Float>(), args[1].arg<CompassDirection>()) }
    }

    private fun getDirection(azimuth: Float, expected: CompassDirection) {
        val bearing = Bearing.from(azimuth)
        assertEquals(expected, bearing.direction)
    }

    @Test
    fun from(){
        for (direction in CompassDirection.entries){
            val bearing = Bearing.from(direction)
            assertEquals(direction.azimuth, bearing.value)
        }
    }

    @Test
    fun value() {
        provideAzimuth().forEach { args -> value(args[0].arg<Float>(), args[1].arg<Float>()) }
    }

    private fun value(azimuth: Float, expected: Float){
        val bearing = Bearing.from(azimuth)
        assertEquals(expected, bearing.value, 0.01f)
    }

    @Test
    fun inverse() {
        provideInverse().forEach { args -> inverse(args[0].arg<Float>(), args[1].arg<Float>()) }
    }

    private fun inverse(azimuth: Float, expected: Float){
        val bearing = Bearing.from(azimuth)
        assertEquals(expected, bearing.inverse().value, 0.01f)
    }

    @Test
    fun withDeclination() {
        val bearing = Bearing.from(45f)
        val dec = bearing.withDeclination(-10f)
        assertEquals(35f, dec.value, 0.01f)
    }

    companion object {
        fun provideDirection(): List<List<Any?>> {
            return listOf(
                listOf(0f, CompassDirection.North),
                listOf(90f, CompassDirection.East),
                listOf(180f, CompassDirection.South),
                listOf(270f, CompassDirection.West),

                listOf(45f, CompassDirection.NorthEast),
                listOf(135f, CompassDirection.SouthEast),
                listOf(225f, CompassDirection.SouthWest),
                listOf(315f, CompassDirection.NorthWest),

                listOf(10f, CompassDirection.North),
                listOf(350f, CompassDirection.North),

                listOf(Float.NaN, CompassDirection.North),
                listOf(Float.NEGATIVE_INFINITY, CompassDirection.North),
                listOf(Float.POSITIVE_INFINITY, CompassDirection.North),
            )
        }
        fun provideAzimuth(): List<List<Any?>> {
            return listOf(
                listOf(0f, 0f),
                listOf(180f, 180f),
                listOf(-10f, 350f),
                listOf(-180f, 180f),
                listOf(710f, 350f),
                listOf(360f, 0f),
                listOf(-710f, 10f),
                listOf(Float.NaN, 0f),
                listOf(Float.NEGATIVE_INFINITY, 0f),
                listOf(Float.POSITIVE_INFINITY, 0f),
            )
        }
        fun provideInverse(): List<List<Any?>> {
            return listOf(
                listOf(0f, 180f),
                listOf(180f, 0f),
                listOf(90f, 270f),
                listOf(270f, 90f),
            )
        }
    }
}