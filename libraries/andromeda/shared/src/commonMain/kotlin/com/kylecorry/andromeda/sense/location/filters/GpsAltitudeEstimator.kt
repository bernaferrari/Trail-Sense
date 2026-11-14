package com.kylecorry.andromeda.sense.location.filters

public interface GpsAltitudeEstimator {
    public val altitude: Float
    public val accuracy: Float?
    public val hasValidReading: Boolean
    public fun update(altitude: Float, accuracy: Float?, fixTimeElapsedNanos: Long)
    public fun reset()
}
