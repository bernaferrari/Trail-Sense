package com.kylecorry.sol.math.filters

public class MedianFilter1D(public val size: Int) : IFilter1D {
    override fun filter(data: List<Float>): List<Float> {
        val realtimeFilter = MedianFilter(size)
        return data.map { realtimeFilter.filter(it) }
    }
}
