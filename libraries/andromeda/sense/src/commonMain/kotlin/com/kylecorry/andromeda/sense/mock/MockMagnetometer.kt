package com.kylecorry.andromeda.sense.mock

import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.sol.math.Vector3

public class MockMagnetometer(
    private val mockMagField: FloatArray = floatArrayOf(
        0f, 1f, 0f
    ), interval: Long = 0
) : MockSensor(interval), Magnetometer {
    override val magneticField: Vector3
        get() = Vector3.from(rawMagneticField)
    override val rawMagneticField: FloatArray = mockMagField
}