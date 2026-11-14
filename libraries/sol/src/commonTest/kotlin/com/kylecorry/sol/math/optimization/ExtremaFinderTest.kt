package com.kylecorry.sol.math.optimization

import kotlin.math.PI

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.Vector2
import kotlin.math.sin

class ExtremaFinderTest {

    @Test
    fun simpleExtremaFinder() {
        provideExtremas().forEach { args -> simpleExtremaFinder(args[0].arg<Double>(), args[1].arg<Double>(), args[2].arg<(x: Double) -> Double>(), args[3].arg<List<Extremum>>()) }
    }

    private fun simpleExtremaFinder(start: Double, end: Double, fn: (x: Double) -> Double, expected: List<Extremum>) {
        val step = 0.01
        val finder = SimpleExtremaFinder(step)
        val actual = finder.find(Range(start, end), fn)
        assertExtremasEqual(expected, actual, step.toFloat(), 0.01f)
    }

    @Test
    fun goldenSearchExtremaFinder() {
        provideExtremas().forEach { args -> goldenSearchExtremaFinder(args[0].arg<Double>(), args[1].arg<Double>(), args[2].arg<(x: Double) -> Double>(), args[3].arg<List<Extremum>>()) }
    }

    private fun goldenSearchExtremaFinder(start: Double, end: Double, fn: (x: Double) -> Double, expected: List<Extremum>) {
        val step = 0.5
        val tolerance = 0.01
        val finder = GoldenSearchExtremaFinder(step, tolerance)
        val actual = finder.find(Range(start, end), fn)
        assertExtremasEqual(expected, actual, tolerance.toFloat() * 2, 0.01f)
    }

    @Test
    fun iterativeSimpleExtremaFinder() {
        provideExtremas().forEach { args -> iterativeSimpleExtremaFinder(args[0].arg<Double>(), args[1].arg<Double>(), args[2].arg<(x: Double) -> Double>(), args[3].arg<List<Extremum>>()) }
    }

    private fun iterativeSimpleExtremaFinder(start: Double, end: Double, fn: (x: Double) -> Double, expected: List<Extremum>) {
        val initialStep = 0.5
        val finalStep = 0.01
        val finder = IterativeSimpleExtremaFinder(initialStep, finalStep, 3)
        val actual = finder.find(Range(start, end), fn)
        assertExtremasEqual(expected, actual, finalStep.toFloat() * 2, 0.01f)
    }

    @Test
    fun noisyExtremaFinderRejectsSubUnitStepForListInput() {
        assertFailsWith<IllegalArgumentException> {
            NoisyExtremaFinder(0.5).find(listOf(1f, 2f, 3f, 4f, 5f))
        }
    }

    private fun assertExtremasEqual(
        expected: List<Extremum>,
        actual: List<Extremum>,
        xTolerance: Float,
        yTolerance: Float
    ) {
        assertEquals(expected.size, actual.size, "Expected ${expected.size} extremas but found ${actual.size}")
        for (i in expected.indices) {
            val expectedExtrema = expected[i]
            val actualExtrema = actual[i]
            assertEquals(actualExtrema.isHigh, expectedExtrema.isHigh)
            assertEquals(actualExtrema.point.x, expectedExtrema.point.x, xTolerance)
            assertEquals(actualExtrema.point.y, expectedExtrema.point.y, yTolerance)
        }
    }

    companion object {
        fun provideExtremas(): List<List<Any?>> {
            return listOf(
                // Sin (min and max)
                listOf(
                    0.0,
                    2 * PI,
                    { x: Double -> sin(x) },
                    listOf(
                        Extremum(
                            Vector2(PI.toFloat() / 2f, 1f),
                            true
                        ),
                        Extremum(
                            Vector2(3 * PI.toFloat() / 2f, -1f),
                            false
                        )
                    )
                ),
                // Sin (max only)
                listOf(
                    0.0,
                    PI,
                    { x: Double -> sin(x) },
                    listOf(
                        Extremum(
                            Vector2(PI.toFloat() / 2f, 1f),
                            true
                        )
                    )
                ),
                // Sin (min only)
                listOf(
                    PI,
                    2 * PI,
                    { x: Double -> sin(x) },
                    listOf(
                        Extremum(
                            Vector2(3 * PI.toFloat() / 2f, -1f),
                            false
                        )
                    )
                ),
                // Flat line
                listOf(
                    0.0,
                    10.0,
                    { x: Double -> 1.0 },
                    emptyList<Extremum>()
                ),
                // Linear
                listOf(
                    0.0,
                    10.0,
                    { x: Double -> x },
                    emptyList<Extremum>()
                ),
                // Parabola
                listOf(
                    0.0,
                    10.0,
                    { x: Double -> x * x },
                    listOf(
                        Extremum(
                            Vector2(0f, 0f),
                            false
                        )
                    )
                ),
            )

        }
    }

}
