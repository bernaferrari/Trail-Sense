package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.sol.math.Quaternion
import com.kylecorry.andromeda.core.sensors.Sensor

interface OrientationSensor: Sensor {
    val orientation: Quaternion
    val rawOrientation: FloatArray
    val headingAccuracy: Float?
}