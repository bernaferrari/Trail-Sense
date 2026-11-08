package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.QuaternionMath
import com.kylecorry.sol.math.Vector3Utils
import com.kylecorry.sol.science.geophysics.Geophysics
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Fuses gyroscope rotation with a gravity and magnetometer attitude. */
class CustomRotationSensor(
    private val magnetometer: IMagnetometer,
    private val accelerometer: IAccelerometer,
    private val gyro: IGyroscope,
    private val scope: CoroutineScope,
    private val gyroWeight: Float = 0.998f,
    private val outOfSyncDotThreshold: Float = 0.85f,
    private val outOfSyncDuration: Duration = 1.seconds,
    private val validMagnetometerMagnitudes: ClosedFloatingPointRange<Float> = 10f..100f,
    private val validAccelerometerMagnitudes: ClosedFloatingPointRange<Float> =
        (0.1f * Geophysics.GRAVITY)..(2f * Geophysics.GRAVITY),
    private val onlyUseMagnetometerQuality: Boolean = false,
    private val maintainStateOnRestart: Boolean = false
) : SharedSensor(), IOrientationSensor {
    private val lock = SynchronizedObject()
    private val geomagnetic = CustomGeomagneticRotationSensor(
        magnetometer, accelerometer, onlyUseMagnetometerQuality, maintainStateOnRestart
    )
    private val reading = Quaternion.zero.toFloatArray()
    private val lastGyro = Quaternion.zero.toFloatArray()
    private val change = FloatArray(4)
    private val gyroQuaternion = FloatArray(4)
    private val previous = FloatArray(4)
    private var initialized = false
    private var geomagneticCount = 0
    private var gyroCount = 0
    private var outOfSyncSince: TimeMark? = null
    private var generation = 0L
    private var job: Job? = null
    private val minCount = 5

    init {
        require(gyroWeight in 0f..1f && outOfSyncDotThreshold in 0f..1f)
        require(outOfSyncDuration.isFinite() && outOfSyncDuration >= Duration.ZERO)
    }

    override val headingAccuracy: Float? get() = null
    override val orientation: Quaternion get() = Quaternion.from(rawOrientation)
    override val rawOrientation: FloatArray get() = synchronized(lock) {
        if (geomagneticCount < minCount || gyroCount < minCount) geomagnetic.rawOrientation
        else reading.copyOf()
    }
    override val hasValidReading: Boolean get() = synchronized(lock) {
        geomagneticCount >= minCount && gyroCount >= minCount &&
            geomagnetic.hasValidReading && gyro.hasValidReading
    }
    override val quality: Quality get() = if (onlyUseMagnetometerQuality) geomagnetic.quality else
        Quality.entries[min(geomagnetic.quality.ordinal, gyro.quality.ordinal)]

    override fun startImpl() {
        val session = synchronized(lock) {
            generation++
            geomagneticCount = 0
            gyroCount = 0
            outOfSyncSince = null
            Quaternion.zero.toFloatArray().copyInto(lastGyro)
            if (!maintainStateOnRestart) {
                initialized = false
                Quaternion.zero.toFloatArray().copyInto(reading)
            }
            generation
        }
        resetSessionMetadata()
        job = scope.launch {
            val updates = Channel<Unit>(Channel.CONFLATED)
            val onGeomagnetic: () -> Boolean = {
                synchronized(lock) {
                    if (session == generation && geomagneticCount < minCount) geomagneticCount++
                }
                true
            }
            val onGyro: () -> Boolean = {
                synchronized(lock) {
                    if (session == generation && gyroCount < minCount) {
                        gyroCount++
                        if (gyroCount == minCount) gyro.rawOrientation.copyInto(lastGyro)
                    }
                }
                updates.trySend(Unit)
                true
            }
            try {
                geomagnetic.start(onGeomagnetic)
                gyro.start(onGyro)
                for (ignored in updates) {
                    if (update(session)) notifyListeners()
                }
            } finally {
                geomagnetic.stop(onGeomagnetic)
                gyro.stop(onGyro)
                updates.close()
            }
        }
    }

    override fun stopImpl() {
        synchronized(lock) { generation++ }
        job?.cancel()
        job = null
    }

    private fun update(session: Long): Boolean = synchronized(lock) {
        if (session != generation || geomagneticCount < minCount || gyroCount < minCount ||
            !geomagnetic.hasValidReading || !gyro.hasValidReading) return@synchronized false
        reading.copyInto(previous)
        val magnetic = geomagnetic.rawOrientation
        QuaternionMath.subtractRotation(gyro.rawOrientation, lastGyro, change)
        gyro.rawOrientation.copyInto(lastGyro)
        QuaternionMath.multiply(reading, change, gyroQuaternion)
        QuaternionMath.normalize(gyroQuaternion, gyroQuaternion)
        val dot = QuaternionMath.dot(gyroQuaternion, magnetic).absoluteValue
        val alpha = if (initialized && dot < outOfSyncDotThreshold) {
            val since = outOfSyncSince ?: TimeSource.Monotonic.markNow().also { outOfSyncSince = it }
            val magneticMagnitude = Vector3Utils.magnitude(magnetometer.rawMagneticField)
            val accelerationMagnitude = Vector3Utils.magnitude(accelerometer.rawAcceleration)
            if (since.elapsedNow() < outOfSyncDuration ||
                magneticMagnitude !in validMagnetometerMagnitudes ||
                accelerationMagnitude !in validAccelerometerMagnitudes) 1f else 0f
        } else if (initialized) {
            outOfSyncSince = null
            gyroWeight
        } else {
            outOfSyncSince = null
            initialized = true
            0f
        }
        QuaternionMath.lerp(magnetic, gyroQuaternion, alpha, reading)
        if (reading.any { !it.isFinite() }) previous.copyInto(reading)
        setEventTimeFrom(if (gyro.eventTimeElapsedNanos >= geomagnetic.eventTimeElapsedNanos) gyro else geomagnetic)
        true
    }
}
