package com.kylecorry.andromeda.sense.barometer

import com.kylecorry.andromeda.core.sensors.Sensor

public interface Barometer: Sensor {
    public val pressure: Float
}