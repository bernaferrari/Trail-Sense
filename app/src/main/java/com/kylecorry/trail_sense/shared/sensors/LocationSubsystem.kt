package com.kylecorry.trail_sense.shared.sensors

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes


import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.altimeter.CachingAltimeterWrapper
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlin.time.Duration
import kotlin.time.Instant
import com.kylecorry.trail_sense.shared.preferences.onEachChange

class LocationSubsystem private constructor(private val context: Context) {

    private val sensorService by lazy { SensorService(context) }
    private val sensorSubsystem by lazy { SensorSubsystem.getInstance(context) }

    private val prefs by lazy { PreferencesSubsystem.getInstance(context).preferences }

    val location: Coordinate
        get() = sensorSubsystem.lastKnownLocation

    val locationAccuracy: Distance?
        get() = sensorSubsystem.lastKnownLocationAccuracy

    val locationAge: Duration
        get() {
            if (isGPSOverridden()) {
                return Duration.ZERO
            }

            val lastUpdate = Instant.fromEpochMilliseconds(
                CacheGPSModule.getCachedData(prefs)?.updateTimeMillis ?: 0
            )
            return (Clock.System.now() - lastUpdate)
        }

    val isLocationStale: Boolean
        get() = locationAge > STALE_LOCATION_AGE

    val elevation: Distance
        get() = sensorSubsystem.lastKnownElevation

    val elevationAge: Duration
        get() {
            if (isAltimeterOverridden()) {
                return Duration.ZERO
            }

            val lastUpdate =
                prefs.getInstant(CachingAltimeterWrapper.LAST_UPDATE_KEY) ?: Instant.fromEpochMilliseconds(0)
            return (Clock.System.now() - lastUpdate)
        }

    init {
        val locationChangedPrefs = listOf(
            CacheGPSModule.LAST_GPS,
            context.getString(R.string.pref_latitude_override),
            context.getString(R.string.pref_longitude_override),
            context.getString(R.string.pref_auto_location),
            // TODO: Technically not true - this should probably by another broadcast
            context.getString(R.string.pref_coordinate_format)
        )
        val elevationChangedPrefs = listOf(
            CachingAltimeterWrapper.LAST_UPDATE_KEY,
            context.getString(R.string.pref_altitude_override),
            context.getString(R.string.pref_altimeter_calibration_mode)
        )
        prefs.onEachChange { key ->
            if (locationChangedPrefs.contains(key)) {
                Tools.broadcast(SensorsToolRegistration.BROADCAST_LOCATION_CHANGED)
            }
            if (elevationChangedPrefs.contains(key)) {
                Tools.broadcast(SensorsToolRegistration.BROADCAST_ELEVATION_CHANGED)
            }
        }
    }

    suspend fun updateLocation() {
        sensorSubsystem.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
    }

    private val userPrefs by lazy { UserPreferences(context) }

    private fun isAltimeterOverridden(): Boolean {
        val mode = userPrefs.altimeterMode
        val usesOverride = mode == UserPreferences.AltimeterMode.Override
        val usesGPS =
            mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.GPS
        return usesOverride || (usesGPS && !sensorService.hasLocationPermission())
    }

    private fun isGPSOverridden(): Boolean {
        return userPrefs.gps.locationSource == GPSLocationSource.Manual || !sensorService.hasLocationPermission()
    }

    companion object {
        private val STALE_LOCATION_AGE: Duration = 2.minutes

        @SuppressLint("StaticFieldLeak")
        private var instance: LocationSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): LocationSubsystem {
            if (instance == null) {
                instance = LocationSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}
