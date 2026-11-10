package com.kylecorry.andromeda.sense.mock

import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.sol.math.Vector3

public class MockAccelerometer(
    private val mockAcceleration: FloatArray = floatArrayOf(
        0f, 0f, 9.80665f
    ), interval: Long = 0
) : MockSensor(interval), Accelerometer {
    override val acceleration: Vector3
        get() = Vector3.from(rawAcceleration)

    override val rawAcceleration: FloatArray = mockAcceleration
}