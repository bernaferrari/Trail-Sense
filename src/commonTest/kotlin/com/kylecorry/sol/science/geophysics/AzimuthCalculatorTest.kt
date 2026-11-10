package com.kylecorry.sol.science.geophysics

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.Vector3

internal class AzimuthCalculatorTest {

    @Test
    fun calculate() {
        provideVectors().forEach { args -> calculate(args[0].arg<Vector3>(), args[1].arg<Vector3>(), args[2].arg<Float?>()) }
    }

    private fun calculate(gravity: Vector3, magnetic: Vector3, expected: Float?) {
        val bearing = AzimuthCalculator.calculate(gravity, magnetic)?.value

        if (bearing == null || expected == null) {
            assertEquals(bearing, expected)
            return
        }

        assertEquals(expected, bearing, 0.5f)

    }

    companion object {
        fun provideVectors(): List<List<Any?>> {
            return listOf(
                // Held flat
                listOf(Vector3(0f, 0f, 9.81f), Vector3(0f, 16f, -51f), 0f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(16f, 0f, -51f), 270f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(-16f, 0f, -51f), 90f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(0f, -16f, -51f), 180f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(8f, -8f, -51f), 225f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(-8f, -8f, -51f), 135f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(-8f, 8f, -51f), 45f),
                listOf(Vector3(0f, 0f, 9.81f), Vector3(8f, 8f, -51f), 315f),
                // Held vertical
                listOf(Vector3(0f, 9.81f, 0f), Vector3(0f, -51f, 16f), 180f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(16f, -51f, 0f), 270f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(-16f, -51f, 0f), 90f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(0f, -51f, -16f), 0f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(8f, -51f, -8f), 315f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(-8f, -51f, -8f), 45f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(-8f, -51f, 8f), 135f),
                listOf(Vector3(0f, 9.81f, 0f), Vector3(8f, -51f, 8f), 225f),
                // In between
                listOf(Vector3(4.1f, 6.8f, 5.6f), Vector3(-35f, -23f, -27f), 58f),
                listOf(Vector3(4.6f, -2.2f, 8.5f), Vector3(-31f, 6f, -34f), 107f),
                // Invalid
                listOf(Vector3(0f, 0f, 0f), Vector3(0f, -51f, 16f), null),
                listOf(Vector3(0f, 0f, 10f), Vector3(0f, 0f, 0f), null),
                listOf(Vector3(10f, 10f, 10f), Vector3(10f, 10f, 10f), null)
            )
        }
    }
}
