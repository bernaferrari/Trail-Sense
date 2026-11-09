package com.kylecorry.trail_sense.shared.sensors.gps

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.permissions.Permissions
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.location.GPS
import com.kylecorry.trail_sense.shared.UserPreferences

enum class GPSSource {
    Override,
    Timezone,
    Device,
    Cache
}

class GPSSourceSelector(context: Context) {

    private val context = context.applicationContext
    private val userPrefs by lazy { UserPreferences(this.context) }

    /**
     * @param useCache true to use the cache instead of the device GPS
     */
    fun getSource(useCache: Boolean): GPSSource {
        val hasPermission = Permissions.canGetFineLocation(context)

        if (userPrefs.gps.locationSource == GPSLocationSource.Manual || (!hasPermission && userPrefs.gps.hasLocationOverride)) {
            return GPSSource.Override
        }

        if (!hasPermission) {
            return GPSSource.Timezone
        }

        if (!useCache && GPS.isAvailable(context)) {
            return GPSSource.Device
        }

        return GPSSource.Cache
    }
}
