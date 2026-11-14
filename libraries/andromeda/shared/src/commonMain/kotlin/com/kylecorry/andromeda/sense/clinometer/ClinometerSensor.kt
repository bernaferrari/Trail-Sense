package com.kylecorry.andromeda.sense.clinometer

import com.kylecorry.andromeda.core.sensors.Sensor

public interface ClinometerSensor : Sensor {
    /**
     * The unit angle in degrees. Front (horizon) is 0, above (sky) is 90, back (horizon) is 180, and below (ground) is 270.
     */
    public val angle: Float

    /**
     * The incline between -90 and 90
     */
    public val incline: Float
}