package com.kylecorry.andromeda.sense.location

import com.kylecorry.sol.units.Distance
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class LocationRequestConfig(
    val frequency: Duration = 20.seconds,
    val minimumDistance: Distance = Distance.meters(0f),
    val minimumFrequency: Duration? = null,
    val maximumUpdateDelay: Duration? = null,
    val maximumUpdates: Int? = null,
    val maximumDuration: Duration? = null,
    val powerUsage: GPSPowerUsage = GPSPowerUsage.High
)
