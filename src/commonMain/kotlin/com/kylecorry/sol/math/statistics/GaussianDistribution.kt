package com.kylecorry.sol.math.statistics

import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.arithmetic.Arithmetic.square
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

public data class GaussianDistribution(val mean: Float, val standardDeviation: Float) {
    val variance: Float = square(standardDeviation)

    public fun probability(x: Float): Float {
        if (Arithmetic.isZero(mean) && Arithmetic.isApproximatelyEqual(standardDeviation, 1f)) {
            // Normal distribution
            return exp(-square(x) / 2) / sqrt(2 * PI).toFloat()
        }
        return exp(-square(x - mean) / (2 * variance)) / sqrt(2 * PI * variance).toFloat()
    }

}
