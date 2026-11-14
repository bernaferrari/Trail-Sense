package com.kylecorry.sol.math.filters

public class LowPassFilter(public var alpha: Float, initialValue: Float = 0f) : IFilter {

    public var value: Float = initialValue

    override fun filter(measurement: Float): Float {
        value = (1 - alpha) * value + alpha * measurement
        return value
    }
}
