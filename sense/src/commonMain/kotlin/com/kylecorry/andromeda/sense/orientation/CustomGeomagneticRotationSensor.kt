package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.min
import kotlin.math.sqrt

/** Computes an east-north-up attitude from gravity and the magnetic field. */
class CustomGeomagneticRotationSensor(
    private val magnetometer: Magnetometer,
    private val accelerometer: Accelerometer,
    private val onlyUseMagnetometerQuality: Boolean = false,
    private val maintainStateOnRestart: Boolean = false
) : SharedSensor(), OrientationSensor {
    private val lock = SynchronizedObject()
    private var reading = Quaternion.zero
    private var hasMagneticReading = false
    private var hasGravityReading = false
    private var hasOrientation = false

    override val headingAccuracy: Float? get() = null
    override val orientation: Quaternion get() = synchronized(lock) { reading }
    override val rawOrientation: FloatArray get() = orientation.toFloatArray()
    override val hasValidReading: Boolean
        get() = synchronized(lock) { hasOrientation } && magnetometer.hasValidReading && accelerometer.hasValidReading
    override val quality: Quality
        get() = if (onlyUseMagnetometerQuality) magnetometer.quality else
            Quality.entries[min(magnetometer.quality.ordinal, accelerometer.quality.ordinal)]

    override fun startImpl() {
        synchronized(lock) {
            if (!maintainStateOnRestart) reading = Quaternion.zero
            hasMagneticReading = false
            hasGravityReading = false
            hasOrientation = false
        }
        resetSessionMetadata()
        magnetometer.start(this::onMagneticReading)
        accelerometer.start(this::onGravityReading)
    }

    override fun stopImpl() {
        magnetometer.stop(this::onMagneticReading)
        accelerometer.stop(this::onGravityReading)
    }

    private fun onMagneticReading(): Boolean {
        synchronized(lock) { hasMagneticReading = true }
        return update()
    }

    private fun onGravityReading(): Boolean {
        synchronized(lock) { hasGravityReading = true }
        return update()
    }

    private fun update(): Boolean {
        if (!synchronized(lock) { hasMagneticReading && hasGravityReading } ||
            !magnetometer.hasValidReading || !accelerometer.hasValidReading) return true
        val attitude = geomagneticAttitude(accelerometer.acceleration, magnetometer.magneticField)
        synchronized(lock) {
            hasOrientation = attitude != null
            if (attitude != null) reading = attitude
        }
        if (attitude == null) return true
        setEventTimeFrom(if (magnetometer.eventTimeElapsedNanos >= accelerometer.eventTimeElapsedNanos) magnetometer else accelerometer)
        notifyListeners()
        return true
    }
}

/** Returns null when gravity or the horizontal magnetic field cannot define a frame. */
internal fun geomagneticAttitude(gravity: Vector3, magnetic: Vector3): Quaternion? {
    val gravityMagnitude = gravity.magnitude()
    val horizontal = magnetic.cross(gravity)
    val horizontalMagnitude = horizontal.magnitude()
    if (!gravityMagnitude.isFinite() || gravityMagnitude < 0.981f ||
        !horizontalMagnitude.isFinite() || horizontalMagnitude < 0.1f) return null
    val up = gravity * (1f / gravityMagnitude)
    val east = horizontal * (1f / horizontalMagnitude)
    val north = up.cross(east)
    // Rows of the matrix map device coordinates into east, north and up.
    val m00 = east.x; val m01 = east.y; val m02 = east.z
    val m10 = north.x; val m11 = north.y; val m12 = north.z
    val m20 = up.x; val m21 = up.y; val m22 = up.z
    val trace = m00 + m11 + m22
    return if (trace > 0f) {
        val s = 2f * sqrt(trace + 1f)
        Quaternion((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, s / 4f)
    } else if (m00 > m11 && m00 > m22) {
        val s = 2f * sqrt(1f + m00 - m11 - m22)
        Quaternion(s / 4f, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
    } else if (m11 > m22) {
        val s = 2f * sqrt(1f + m11 - m00 - m22)
        Quaternion((m01 + m10) / s, s / 4f, (m12 + m21) / s, (m02 - m20) / s)
    } else {
        val s = 2f * sqrt(1f + m22 - m00 - m11)
        Quaternion((m02 + m20) / s, (m12 + m21) / s, s / 4f, (m10 - m01) / s)
    }
}
