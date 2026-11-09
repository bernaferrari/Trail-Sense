package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer
import kotlin.math.abs
import kotlin.math.withSign

/** Classifies the device orientation from a gravity or low-pass accelerometer. */
class DeviceOrientation(private val accelerometer: Accelerometer) : SharedSensor() {
    override fun startImpl() {
        gotReading = false
        resetSessionMetadata()
        accelerometer.start(this::onAccelerometer)
    }

    override fun stopImpl() {
        accelerometer.stop(this::onAccelerometer)
    }

    override val hasValidReading: Boolean
        get() = gotReading

    var orientation: Orientation = Orientation.Flat
        private set

    private var gotReading = false

    private fun onAccelerometer(): Boolean {
        val acceleration = accelerometer.rawAcceleration
        var largestAccelAxis = 0
        for (i in acceleration.indices) {
            if (abs(acceleration[i]) > abs(acceleration[largestAccelAxis])) {
                largestAccelAxis = i
            }
        }

        largestAccelAxis = (largestAccelAxis + 1).toDouble()
            .withSign(acceleration[largestAccelAxis].toDouble()).toInt()

        orientation = when (largestAccelAxis) {
            -3 -> Orientation.FlatInverse
            -2 -> Orientation.PortraitInverse
            -1 -> Orientation.LandscapeInverse
            1 -> Orientation.Landscape
            2 -> Orientation.Portrait
            else -> Orientation.Flat
        }

        gotReading = true

        setEventTimeFrom(accelerometer)
        notifyListeners()
        return true
    }

    enum class Orientation {
        Portrait,
        PortraitInverse,
        Flat,
        FlatInverse,
        Landscape,
        LandscapeInverse
    }

}