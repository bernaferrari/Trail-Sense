package com.kylecorry.sol.math.statistics

import kotlin.test.Test
import kotlin.test.assertEquals

class GaussianDistributionTest {

    @Test
    fun getVariance() {
        getVariance(0f, 1f, 1f)
        getVariance(0f, 2f, 4f)
        getVariance(1f, 1f, 1f)
        getVariance(1f, 2f, 4f)
        getVariance(0f, 4f, 16f)
    }

    private fun getVariance(mean: Float, standardDeviation: Float, expected: Float) {
        val distribution = GaussianDistribution(mean, standardDeviation)
        assertEquals(expected, distribution.variance)
    }

    @Test
    fun probability() {
        probability(0f, 1f, 0f, 0.3989423f)
        probability(0f, 1f, 1f, 0.2419707f)
        probability(0f, 1f, 2f, 0.05399097f)
        probability(0f, 1f, 3f, 0.004431848f)
        probability(0f, 1f, 4f, 0.0001338302f)
        probability(0f, 1f, 5f, 1.48672E-06f)
        probability(1f, 1f, 0f, 0.2419707f)
        probability(1f, 1f, 1f, 0.3989423f)
        probability(1f, 1f, 2f, 0.2419707f)
    }

    private fun probability(mean: Float, standardDeviation: Float, x: Float, expected: Float) {
        val distribution = GaussianDistribution(mean, standardDeviation)
        assertEquals(expected, distribution.probability(x), 0.0001f)
    }
}