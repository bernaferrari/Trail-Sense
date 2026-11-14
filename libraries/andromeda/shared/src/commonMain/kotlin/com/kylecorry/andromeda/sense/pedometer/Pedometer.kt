package com.kylecorry.andromeda.sense.pedometer

import com.kylecorry.andromeda.core.sensors.Sensor

public interface Pedometer: Sensor {
    public val steps: Int
}