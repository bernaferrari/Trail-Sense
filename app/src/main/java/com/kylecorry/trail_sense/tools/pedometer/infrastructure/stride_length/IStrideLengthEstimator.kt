package com.kylecorry.trail_sense.tools.pedometer.infrastructure.stride_length

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.sol.units.Distance

interface IStrideLengthEstimator : Sensor {
    val strideLength: Distance?
    fun reset()
}
