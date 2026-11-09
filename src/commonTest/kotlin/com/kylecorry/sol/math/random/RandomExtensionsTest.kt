package com.kylecorry.sol.math.random

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import com.kylecorry.sol.math.statistics.Statistics
import kotlin.random.Random

internal class RandomExtensionsTest {

    @Test
    fun nextGaussianHasBoundedAttempts() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int {
                return 0
            }
        }

        assertFailsWith<IllegalStateException> {
            random.nextGaussian()
        }
    }

    @Test
    fun nextGaussian() {
        val random = Random(12345)
        val samples = List(100) { random.nextGaussian().toFloat() }
        val mean = Statistics.mean(samples)
        val stdDev = Statistics.stdev(samples, mean = mean)

        assertEquals(0f, mean, 0.1f)
        assertEquals(1f, stdDev, 0.1f)
    }
}
