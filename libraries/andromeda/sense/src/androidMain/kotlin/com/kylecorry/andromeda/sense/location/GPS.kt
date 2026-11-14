package com.kylecorry.andromeda.sense.location

import com.kylecorry.andromeda.sense.location.LocationRequestConfig

import android.content.Context
import android.location.LocationManager

public class GPS(
    context: Context,
    requestConfig: LocationRequestConfig = LocationRequestConfig()
) : BaseGPS(
    context,
    LocationManager.GPS_PROVIDER,
    requestConfig
) {
    public companion object {
        public fun isAvailable(context: Context): Boolean {
            return isAvailable(context, LocationManager.GPS_PROVIDER)
        }
    }
}
