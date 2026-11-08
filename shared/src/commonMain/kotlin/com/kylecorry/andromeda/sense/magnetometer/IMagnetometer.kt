package com.kylecorry.andromeda.sense.magnetometer

import com.kylecorry.sol.math.Vector3
import com.kylecorry.andromeda.core.sensors.Sensor

interface IMagnetometer: Sensor {
    val magneticField: Vector3
    val rawMagneticField: FloatArray
}