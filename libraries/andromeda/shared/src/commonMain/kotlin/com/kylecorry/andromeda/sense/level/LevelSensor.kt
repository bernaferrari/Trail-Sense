package com.kylecorry.andromeda.sense.level

import com.kylecorry.andromeda.core.sensors.Sensor

public interface LevelSensor : Sensor {
    /**
     * The X level in degrees
     */
    public val x: Float

    /**
     * The Y level in degrees
     */
    public val y: Float
}