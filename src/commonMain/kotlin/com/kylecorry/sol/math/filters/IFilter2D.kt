package com.kylecorry.sol.math.filters

import com.kylecorry.sol.math.Vector2

public interface IFilter2D {
    public fun filter(data: List<Vector2>): List<Vector2>
}
