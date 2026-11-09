package com.kylecorry.trail_sense.shared.sensors.thermometer

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.sol.units.Temperature

class CalibratedThermometerWrapper(
    private val thermometer: Thermometer,
    private val calibrator: ITemperatureCalibrator
) : Thermometer, SharedSensor() {
    override val hasValidReading: Boolean
        get() = thermometer.hasValidReading

    override val temperature: Float
        get() = calibrate(thermometer.temperature)

    override fun startImpl() {
        thermometer.start(this::onReading)
    }

    override fun stopImpl() {
        thermometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }

    private fun calibrate(temperature: Float): Float {
        return calibrator.calibrate(Temperature.celsius(temperature)).value
    }
}
