package com.kylecorry.sol.math.optimization

public interface IListExtremaFinder {
    public fun find(values: List<Float>): List<Extremum>
}
