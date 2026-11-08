package com.kylecorry.andromeda.sense.accelerometer

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldAccelerometerTest {
    private class Accelerometer : SharedSensor(), IAccelerometer {
        var running = false
        override val hasValidReading = true
        override val rawAcceleration = floatArrayOf(1f, 0f, 0f)
        override val acceleration get() = Vector3(rawAcceleration[0], rawAcceleration[1], rawAcceleration[2])
        override fun startImpl() { running = true }
        override fun stopImpl() { running = false }
    }

    private class Orientation : SharedSensor(), IOrientationSensor {
        var running = false
        private val half = sqrt(0.5f)
        override val rawOrientation = floatArrayOf(0f, 0f, half, half)
        override val orientation get() = Quaternion(0f, 0f, half, half)
        override val headingAccuracy: Float? = null
        override val hasValidReading = true
        override fun startImpl() { running = true }
        override fun stopImpl() { running = false }
    }

    @Test
    fun rotatesReadingsAndStopsUnderlyingSensors() = runBlocking {
        val accelerometer = Accelerometer()
        val orientation = Orientation()
        val sensor = WorldAccelerometer(accelerometer, orientation, this)
        val reading = CompletableDeferred<Vector3>()
        val listener = { reading.complete(sensor.acceleration); true }
        sensor.start(listener)
        val result = withTimeout(1000) { reading.await() }
        assertEquals(0f, result.x, 0.001f)
        assertEquals(1f, result.y, 0.001f)
        assertTrue(accelerometer.running)
        assertTrue(orientation.running)
        sensor.stop(listener)
        withTimeout(1000) {
            while (accelerometer.running || orientation.running) kotlinx.coroutines.yield()
        }
        assertFalse(accelerometer.running)
        assertFalse(orientation.running)
    }
}
