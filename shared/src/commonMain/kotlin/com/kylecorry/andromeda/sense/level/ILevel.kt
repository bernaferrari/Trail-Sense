package com.kylecorry.andromeda.sense.level

import com.kylecorry.andromeda.core.sensors.Sensor

interface ILevel : Sensor {
    /**
     * The X level in degrees
     */
    val x: Float

    /**
     * The Y level in degrees
     */
    val y: Float
}