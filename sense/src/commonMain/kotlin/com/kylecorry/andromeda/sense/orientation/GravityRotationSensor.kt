package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** Estimates tilt from gravity. Gravity alone cannot determine a magnetic heading. */
public class GravityRotationSensor(
    private val accelerometer: Accelerometer,
    private val maintainStateOnRestart: Boolean = false
) : SharedSensor(), OrientationSensor {
    private val lock = SynchronizedObject()
    private var reading = Quaternion.zero
    private var valid = false

    override val headingAccuracy: Float? get() = null
    override val orientation: Quaternion get() = synchronized(lock) { reading }
    override val rawOrientation: FloatArray get() = orientation.toFloatArray()
    override val hasValidReading: Boolean get() = synchronized(lock) { valid } && accelerometer.hasValidReading
    override val quality: Quality get() = accelerometer.quality

    override fun startImpl() {
        synchronized(lock) {
            if (!maintainStateOnRestart) reading = Quaternion.zero
            valid = false
        }
        resetSessionMetadata()
        accelerometer.start(this::onSensorUpdate)
    }

    override fun stopImpl(): Unit = accelerometer.stop(this::onSensorUpdate)

    private fun onSensorUpdate(): Boolean {
        val attitude = if (accelerometer.hasValidReading) gravityAttitude(accelerometer.acceleration) else null
        synchronized(lock) {
            valid = attitude != null
            if (attitude != null) reading = attitude
        }
        if (attitude != null) {
            setEventTimeFrom(accelerometer)
            notifyListeners()
        }
        return true
    }
}

/** Finds the shortest rotation mapping the device gravity vector to world up. */
internal fun gravityAttitude(gravity: Vector3): Quaternion? {
    val magnitude = gravity.magnitude()
    if (!magnitude.isFinite() || magnitude < 0.981f) return null
    val x = gravity.x / magnitude
    val y = gravity.y / magnitude
    val z = gravity.z / magnitude
    if (z < -0.999999f) return Quaternion(1f, 0f, 0f, 0f)
    return Quaternion(y, -x, 0f, 1f + z).normalize()
}
