package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode

class GPSPreferences(preferences: Preferences) : PreferenceRepo(preferences), IGPSPreferences {

    override var locationSource by StringEnumPreference(
        cache,
        PreferenceKeys.autoLocation,
        GPSLocationSource.entries.associateBy { it.id },
        GPSLocationSource.GPS
    )

    override var accuracyFilter: GPSAccuracyFilter
        get() {
            val accuracy = cache.getFloat(PreferenceKeys.gpsAccuracyMeters)
                ?: GPSAccuracyFilter.Default.minAccuracy!!
            val wait = cache.getInt(PreferenceKeys.gpsAccuracyWaitSeconds)
                ?: GPSAccuracyFilter.Default.maxAccuracyWait!!.inWholeSeconds.toInt()
            if (accuracy <= 0 || wait <= 0) {
                return GPSAccuracyFilter.None
            }
            return GPSAccuracyFilter.custom(accuracy, wait)
        }
        set(value) {
            val accuracy = value.minAccuracy
            val maxWait = value.maxAccuracyWait
            if (accuracy == null || maxWait == null) {
                cache.putFloat(PreferenceKeys.gpsAccuracyMeters, 0f)
                cache.putInt(PreferenceKeys.gpsAccuracyWaitSeconds, 0)
                return
            }
            val wait = maxWait.inWholeSeconds.toInt()
            cache.putFloat(
                PreferenceKeys.gpsAccuracyMeters,
                accuracy.coerceIn(
                    GPSAccuracyFilter.MIN_ACCURACY_METERS.toFloat(),
                    GPSAccuracyFilter.MAX_ACCURACY_METERS.toFloat()
                )
            )
            cache.putInt(
                PreferenceKeys.gpsAccuracyWaitSeconds,
                wait.coerceIn(
                    GPSAccuracyFilter.MIN_WAIT_SECONDS,
                    GPSAccuracyFilter.MAX_WAIT_SECONDS
                )
            )
        }

    override val smoothing: Int
        get() = (cache.getInt(PreferenceKeys.gpsSmoothing) ?: 0).coerceIn(0, 100)

    override val powerMode by StringEnumPreference(
        cache,
        PreferenceKeys.gpsPowerUsage,
        GPSPowerMode.entries.associateBy { it.id.toString() },
        GPSPowerMode.High
    )

    override var locationOverride: Coordinate
        get() {
            val latStr = cache.getString(PreferenceKeys.latitudeOverride) ?: "0.0"
            val lngStr = cache.getString(PreferenceKeys.longitudeOverride) ?: "0.0"

            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            return Coordinate(lat, lng)
        }
        set(value) {
            cache.putString(PreferenceKeys.latitudeOverride, value.latitude.toString())
            cache.putString(PreferenceKeys.longitudeOverride, value.longitude.toString())
        }

    override val hasLocationOverride: Boolean
        get() = cache.contains(PreferenceKeys.latitudeOverride) &&
                cache.contains(PreferenceKeys.longitudeOverride)
}
