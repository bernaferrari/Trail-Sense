package com.kylecorry.sol.math.filters

public class MovingAverageFilter1D(public var size: Int) : IFilter1D {
    override fun filter(data: List<Float>): List<Float> {
        val filter = MovingAverageFilter(size)
        return data.map { filter.filter(it) }
    }
}
