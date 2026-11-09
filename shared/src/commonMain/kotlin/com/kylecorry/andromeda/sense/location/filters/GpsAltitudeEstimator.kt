package com.kylecorry.andromeda.sense.location.filters

interface GpsAltitudeEstimator {
    val altitude: Float
    val accuracy: Float?
    val hasValidReading: Boolean
    fun update(altitude: Float, accuracy: Float?, fixTimeElapsedNanos: Long)
    fun reset()
}
