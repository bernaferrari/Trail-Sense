package com.kylecorry.andromeda.sense.altitude

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.sense.barometer.Barometer
import kotlin.math.pow
import com.kylecorry.sol.units.Pressure

public class BarometricAltimeter(
    private val barometer: Barometer,
    public var seaLevelPressure: Pressure = Pressure.hpa(1013.25f)
) : SharedSensor(), Altimeter {

    override fun startImpl() {
        barometer.start(this::onSensorUpdate)
    }

    override fun stopImpl() {
        barometer.stop(this::onSensorUpdate)
    }

    override val altitude: Float
        get() = 44330f * (1f - (barometer.pressure / seaLevelPressure.hpa().value).pow(0.19029495f))

    override val hasValidReading: Boolean
        get() = barometer.hasValidReading

    private fun onSensorUpdate(): Boolean {
        setEventTimeFrom(barometer)
        notifyListeners()
        return true
    }
}