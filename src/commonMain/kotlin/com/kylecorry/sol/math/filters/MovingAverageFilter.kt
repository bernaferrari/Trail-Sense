package com.kylecorry.sol.math.filters

public class MovingAverageFilter(public var size: Int) : IFilter {

    private val window = mutableListOf<Float>()

    override fun filter(measurement: Float): Float {
        window.add(measurement)
        while (window.size > size && window.isNotEmpty()) {
            window.removeAt(0)
        }
        return window.average().toFloat()
    }
}
