package com.kylecorry.trail_sense.shared.sensors.gps

import kotlinx.serialization.Serializable
import com.kylecorry.trail_sense.shared.ProguardIgnore

@Serializable
data class GPSKalmanState(
    val state: List<Float> = emptyList(),
    val covariance: List<List<Float>> = emptyList(),
    val referenceLatitude: Double = 0.0,
    val referenceLongitude: Double = 0.0
) : ProguardIgnore
