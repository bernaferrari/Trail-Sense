package com.kylecorry.sol.math.regression

import com.kylecorry.sol.math.Vector2

public interface ITimeSeriesPredictor {
    public fun predictNext(samples: List<Vector2>, n: Int, step: Float? = null): List<Vector2>
}
