package com.kylecorry.andromeda.sense.location

public interface SatelliteStatusSensor {
    /**
     * The number of satellites used to calculate the location
     */
    public val satellites: Int?

    /**
     * The details of the satellites detected by the GNSS receiver
     */
    public val satelliteDetails: List<Satellite>?
}
