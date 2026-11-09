package com.kylecorry.andromeda.sense.accelerometer

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.sol.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LowPassAccelerometerTest {
    private class Input : SharedSensor(), Accelerometer {
        override val hasValidReading = true
        override val rawAcceleration = FloatArray(3)
        override val acceleration get() = Vector3(rawAcceleration[0], rawAcceleration[1], rawAcceleration[2])
        override fun startImpl() = Unit
        override fun stopImpl() = Unit
        fun emit(x: Float) { rawAcceleration[0] = x; notifyListeners() }
    }

    @Test
    fun filtersAndResetsOnRestart() {
        val input = Input()
        val sensor = LowPassAccelerometer(input, filterSize = 0.5f)
        val listener = { true }
        sensor.start(listener)
        input.emit(8f)
        assertEquals(4f, sensor.acceleration.x)
        input.emit(8f)
        assertEquals(6f, sensor.acceleration.x)
        sensor.stop(listener)
        sensor.start(listener)
        assertFalse(sensor.hasValidReading)
        input.emit(8f)
        assertEquals(4f, sensor.acceleration.x)
        sensor.stop(listener)
    }
}
