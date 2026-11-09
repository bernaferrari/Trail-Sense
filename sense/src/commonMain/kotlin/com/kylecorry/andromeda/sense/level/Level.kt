package com.kylecorry.andromeda.sense.level

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.andromeda.sense.orientation.OrientationUtils

class Level(
    private val orientationSensor: OrientationSensor,
    var surfaceRotation: Int = 0,
) :
    SharedSensor(), LevelSensor {

    private val rotationMatrix = FloatArray(16)
    private val orientation = FloatArray(3)

    /**
     * The X level in degrees
     */
    override var x: Float = 0f
        private set

    /**
     * The Y level in degrees
     */
    override var y: Float = 0f
        private set

    override val hasValidReading: Boolean
        get() = orientationSensor.hasValidReading

    override fun startImpl() {
        orientationSensor.start(this::onSensorUpdate)
    }

    override fun stopImpl() {
        orientationSensor.stop(this::onSensorUpdate)
    }

    private fun onSensorUpdate(): Boolean {
        OrientationUtils.getCompassOrientation(
            orientationSensor,
            rotationMatrix,
            orientation,
            surfaceRotation
        )

        x = -orientation[2].coerceIn(-90f, 90f)
        y = -orientation[1].coerceIn(-90f, 90f)

        setEventTimeFrom(orientationSensor)
        notifyListeners()
        return true
    }
}