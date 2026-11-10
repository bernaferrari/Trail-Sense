package com.kylecorry.andromeda.sense.accelerometer

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationUtils
import com.kylecorry.sol.math.Vector3
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.min

/**
 * An accelerometer which reports acceleration in the world frame (x is east, y is north, z is up)
 * @param accelerometer The accelerometer to use
 * @param orientation The orientation sensor to use
 * @param scope Scope controlling sensor collection
 * @param interval The update interval
 * @param useTrueNorth True to use true north, false to use magnetic north
 * @param surfaceRotation The display rotation: 0, 1, 2 or 3 quarter turns
 * @param useAROrientation True to use AR orientation, false to use compass orientation
 */
public class WorldAccelerometer(
    private val accelerometer: Accelerometer,
    private val orientation: OrientationSensor,
    private val scope: CoroutineScope,
    private val interval: Duration = 20.milliseconds,
    public var useTrueNorth: Boolean = false,
    public var surfaceRotation: Int = 0,
    public var useAROrientation: Boolean = false
) : Accelerometer, SharedSensor() {

    private val rotationMatrix = FloatArray(16)
    private val tempOrientation = FloatArray(3)

    /**
     * The geomagnetic declination
     */
    public var declination: Float = 0f

    override val acceleration: Vector3
        get() = Vector3(rawAcceleration[0], rawAcceleration[1], rawAcceleration[2])

    override val rawAcceleration: FloatArray = FloatArray(3)

    override val hasValidReading: Boolean
        get() = accelerometer.hasValidReading && orientation.hasValidReading

    private var job: Job? = null

    init {
        require(interval.isFinite() && interval > Duration.ZERO)
    }

    override fun startImpl() {
        job = scope.launch {
            // A distinct listener per job keeps a cancelled job from unsubscribing a new one.
            val listener = object : () -> Boolean {
                override fun invoke() = true
            }
            try {
                accelerometer.start(listener)
                orientation.start(listener)
                while (isActive) {
                    update()
                    delay(interval)
                }
            } finally {
                accelerometer.stop(listener)
                orientation.stop(listener)
            }
        }
    }

    override fun stopImpl() {
        job?.cancel()
        job = null
    }

    override val quality: Quality
        get() = Quality.entries[min(accelerometer.quality.ordinal, orientation.quality.ordinal)]

    private fun update() {
        if (!hasValidReading) return

        if (useAROrientation) {
            OrientationUtils.getAROrientation(
                orientation,
                rotationMatrix,
                tempOrientation,
                surfaceRotation,
                if (useTrueNorth) declination else 0f
            )
        } else {
            OrientationUtils.getCompassOrientation(
                orientation,
                rotationMatrix,
                tempOrientation,
                surfaceRotation,
                if (useTrueNorth) declination else 0f
            )
        }

        val source = accelerometer.rawAcceleration
        for (row in 0..2) {
            val offset = row * 4
            rawAcceleration[row] = rotationMatrix[offset] * source[0] +
                rotationMatrix[offset + 1] * source[1] + rotationMatrix[offset + 2] * source[2]
        }

        val latest = if (accelerometer.eventTimeElapsedNanos >= orientation.eventTimeElapsedNanos) {
            accelerometer
        } else {
            orientation
        }
        setEventTimeFrom(latest)
        notifyListeners()
    }
}
