package com.kylecorry.trail_sense.shared.andromeda_temp.sense.location

import com.kylecorry.andromeda.sense.location.LocationRequestConfig

import android.content.Context
import android.location.LocationManager

class GPS(
    context: Context,
    requestConfig: LocationRequestConfig = LocationRequestConfig()
) : BaseGPS(
    context,
    LocationManager.GPS_PROVIDER,
    requestConfig
) {
    companion object {
        fun isAvailable(context: Context): Boolean {
            return isAvailable(context, LocationManager.GPS_PROVIDER)
        }
    }
}
