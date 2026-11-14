package com.kylecorry.andromeda.sense.hygrometer

import com.kylecorry.andromeda.core.sensors.Sensor

public interface Hygrometer: Sensor {
    public val humidity: Float
}