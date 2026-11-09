package com.kylecorry.trail_sense.shared.andromeda_temp.sense.location

import android.location.Location
import androidx.core.location.LocationListenerCompat

internal class SimpleLocationListener(private val onLocationChangedFn: (location: Location?) -> Unit) :
    LocationListenerCompat {
    override fun onLocationChanged(location: Location) {
        onLocationChangedFn.invoke(location)
    }
}
