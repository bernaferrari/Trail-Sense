package com.kylecorry.andromeda.sense.light

import com.kylecorry.andromeda.core.sensors.Sensor

public interface LightSensor: Sensor {
    public val illuminance: Float
}