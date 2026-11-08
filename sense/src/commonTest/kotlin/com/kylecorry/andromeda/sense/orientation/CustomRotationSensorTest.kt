package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomRotationSensorTest {
    private class Accelerometer : SharedSensor(), IAccelerometer {
        override val hasValidReading = true
        override val rawAcceleration = floatArrayOf(0f, 0f, 9.81f)
        override val acceleration get() = Vector3.from(rawAcceleration)
        override fun startImpl() = Unit
        override fun stopImpl() = Unit
        fun emit() = notifyListeners()
    }
    private class Magnetometer : SharedSensor(), IMagnetometer {
        override val hasValidReading = true
        override val rawMagneticField = floatArrayOf(0f, 50f, 0f)
        override val magneticField get() = Vector3.from(rawMagneticField)
        override fun startImpl() = Unit
        override fun stopImpl() = Unit
        fun emit() = notifyListeners()
    }
    private class Gyro : SharedSensor(), IGyroscope {
        override val hasValidReading = true
        override var orientation = Quaternion.zero
        override val rawOrientation get() = orientation.toFloatArray()
        override val headingAccuracy: Float? = null
        override val angularRate = Euler(0f, 0f, 0f)
        override val rawAngularRate = FloatArray(3)
        override fun startImpl() = Unit
        override fun stopImpl() = Unit
        fun emit() = notifyListeners()
    }

    @Test
    fun fusesGyroRotationAfterInitialAttitude() = runBlocking {
        val acceleration = Accelerometer()
        val magnetic = Magnetometer()
        val gyro = Gyro()
        val sensor = CustomRotationSensor(magnetic, acceleration, gyro, this, gyroWeight = 1f)
        val initialized = CompletableDeferred<Unit>()
        val rotated = CompletableDeferred<Quaternion>()
        val listener = {
            if (!initialized.isCompleted) initialized.complete(Unit)
            else if (gyro.orientation.z != 0f) rotated.complete(sensor.orientation)
            true
        }
        sensor.start(listener)
        yield()
        repeat(5) { magnetic.emit(); acceleration.emit() }
        repeat(5) { gyro.emit() }
        withTimeout(1000) { initialized.await() }
        assertTrue(sensor.hasValidReading)
        val half = sqrt(0.5f)
        gyro.orientation = Quaternion(0f, 0f, half, half)
        gyro.emit()
        val result = withTimeout(1000) { rotated.await() }
        assertEquals(half, result.z, 0.0001f)
        assertEquals(half, result.w, 0.0001f)
        sensor.stop(listener)
    }
}
