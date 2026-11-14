package com.kylecorry.sol.math.optimization

import com.kylecorry.sol.math.Range

public interface IOptimizer {
    public fun optimize(
        xRange: Range<Double>,
        yRange: Range<Double> = Range(0.0, 0.0),
        maximize: Boolean = true,
        fn: (x: Double, y: Double) -> Double
    ): Pair<Double, Double>
}
