package com.kylecorry.andromeda.sense.location

import androidx.core.location.LocationRequestCompat

public fun GPSPowerUsage.toLocationRequestQuality(): Int = when (this) {
    GPSPowerUsage.High -> LocationRequestCompat.QUALITY_HIGH_ACCURACY
    GPSPowerUsage.Balanced -> LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
    GPSPowerUsage.Low -> LocationRequestCompat.QUALITY_LOW_POWER
}
