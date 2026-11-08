package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.sol.math.Quaternion
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class OrientationUtilsTest {
    private class Orientation(var vector: FloatArray) : SharedSensor(), IOrientationSensor {
        override val rawOrientation get() = vector
        override val orientation get() = Quaternion(vector[0], vector[1], vector[2], vector[3])
        override val headingAccuracy: Float? get() = null
        override val hasValidReading get() = true
        override fun startImpl() = Unit
        override fun stopImpl() = Unit
    }

    @Test
    fun compassRotationAndDeclination() {
        val sensor = Orientation(floatArrayOf(0f, 0f, 0f, 1f))
        val result = FloatArray(3)
        for (size in listOf(9, 16)) {
            val matrix = FloatArray(size)
            OrientationUtils.getCompassOrientation(sensor, matrix, result)
            assertEquals(0f, result[0], 0.001f)
            OrientationUtils.getCompassOrientation(sensor, matrix, result, surfaceRotation = 1)
            assertEquals(90f, result[0], 0.001f)
            OrientationUtils.getCompassOrientation(sensor, matrix, result, declination = 90f)
            assertEquals(90f, result[0], 0.001f)
            val half = sqrt(0.5f)
            sensor.vector = floatArrayOf(0f, 0f, half, half)
            OrientationUtils.getCompassOrientation(sensor, matrix, result)
            assertEquals(-90f, result[0], 0.001f)
            sensor.vector = floatArrayOf(0f, 0f, 0f, 1f)
        }
    }
}
