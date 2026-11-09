package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.orientation.Gyroscope
import com.kylecorry.sol.math.Euler
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Instant

/** Integrates angular velocity in degrees per second into relative orientation. */
class FlowGyroscope(
    private val readings: Flow<Reading<Vector3>>,
    private val scope: CoroutineScope,
    private val maintainStateOnRestart: Boolean = false,
) : SharedSensor(), Gyroscope {
    private val lock = SynchronizedObject()
    private var quaternion = Quaternion.zero
    private var rate = Vector3.zero
    private var lastTime: Instant? = null
    private var valid = false
    private var generation = 0L
    private var collection: Job? = null

    override val orientation: Quaternion get() = synchronized(lock) { quaternion }
    override val rawOrientation: FloatArray get() = orientation.toFloatArray()
    override val angularRate: Euler get() = synchronized(lock) { Euler(rate.x, rate.y, rate.z) }
    override val rawAngularRate: FloatArray get() = synchronized(lock) { floatArrayOf(rate.x, rate.y, rate.z) }
    override val headingAccuracy: Float? get() = null
    override val hasValidReading: Boolean get() = synchronized(lock) { valid }

    override fun startImpl() {
        val session = synchronized(lock) {
            generation++
            if (!maintainStateOnRestart) quaternion = Quaternion.zero
            rate = Vector3.zero
            lastTime = null
            valid = false
            generation
        }
        resetSessionMetadata()
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                val accepted = synchronized(lock) {
                    if (session != generation) return@synchronized false
                    val value = reading.value
                    if (!value.x.isFinite() || !value.y.isFinite() || !value.z.isFinite()) return@synchronized false
                    val previous = lastTime
                    if (previous != null && reading.time < previous) return@synchronized false
                    lastTime = reading.time
                    rate = value
                    if (previous != null && reading.time > previous) {
                        val seconds = (reading.time - previous).inWholeNanoseconds.toDouble() / 1e9
                        val speed = value.magnitude().toDouble() * PI / 180.0
                        if (speed > 0.0 && speed.isFinite()) {
                            val halfAngle = speed * seconds / 2.0
                            val scale = (sin(halfAngle) / value.magnitude()).toFloat()
                            val delta = Quaternion(value.x * scale, value.y * scale, value.z * scale, cos(halfAngle).toFloat())
                            quaternion = (quaternion * delta).normalize()
                        }
                    }
                    valid = true
                    true
                }
                if (accepted) {
                    setEventTime(reading.time)
                    notifyListeners()
                }
            }
        }
        collection?.start()
    }

    override fun stopImpl() {
        synchronized(lock) { generation++ }
        collection?.cancel()
        collection = null
    }
}
