package com.kylecorry.andromeda.sense.compass

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.orientation.IOrientationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationUtils
import com.kylecorry.sol.units.Bearing

/**
 * A compass sensor
 * @param orientationSensor The orientation sensor to use
 * @param isAugmentedReality True if the compass should be in AR mode (device held vertically)
 * @param surfaceRotation The surface rotation of the device (0, 1, 2, 3)
 * @param offset The offset to apply to the bearing (in addition to declination, in degrees)
 */
class Compass(
    private val orientationSensor: IOrientationSensor,
    var useTrueNorth: Boolean = false,
    var isAugmentedReality: Boolean = false,
    var surfaceRotation: Int = 0,
    var offset: Float = 0f
) : SharedSensor(), ICompass {

    private var _bearing = 0f
    private val orientation = FloatArray(3)
    private val rotationMatrix = FloatArray(16)

    override fun startImpl() {
        orientationSensor.start(this::onSensorUpdate)
    }

    override fun stopImpl() {
        orientationSensor.stop(this::onSensorUpdate)
    }

    override val bearing: Bearing
        get() = Bearing.from(rawBearing)

    override var declination: Float = 0f

    override val hasValidReading: Boolean
        get() = orientationSensor.hasValidReading

    override val rawBearing: Float
        get() = Bearing.getBearing(Bearing.getBearing(_bearing) + if (useTrueNorth) declination else 0f)

    override val quality: Quality
        get() = orientationSensor.quality

    private fun onSensorUpdate(): Boolean {
        _bearing = if (isAugmentedReality) {
            OrientationUtils.getAROrientation(
                orientationSensor,
                rotationMatrix,
                orientation,
                surfaceRotation
            )
            orientation[0]
        } else {
            OrientationUtils.getCompassOrientation(
                orientationSensor,
                rotationMatrix,
                orientation,
                surfaceRotation
            )
            orientation[0]
        } + offset
        setEventTimeFrom(orientationSensor)
        notifyListeners()
        return true
    }
}
