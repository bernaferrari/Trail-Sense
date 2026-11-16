package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.preferences.onEachChange
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Shared [ILocationSubsystem] exposing last known coordinates, elevation, staleness, and preference change broadcasts.
 */
class SharedLocationSubsystem(
    private val sensorService: SensorService,
    private val sensorSubsystem: ISensorSubsystem,
    private val preferences: Preferences,
    private val userPrefs: UserPreferences
) : ILocationSubsystem {

    override val location: Coordinate
        get() = sensorSubsystem.lastKnownLocation

    override val locationAccuracy: Distance?
        get() = sensorSubsystem.lastKnownLocationAccuracy

    override val locationAge: Duration
        get() {
            if (isGPSOverridden()) {
                return Duration.ZERO
            }
            val lastUpdate = Instant.fromEpochMilliseconds(
                SharedSensorCache.readGps(preferences)?.updateTimeMillis ?: 0L
            )
            return Clock.System.now() - lastUpdate
        }

    override val isLocationStale: Boolean
        get() = locationAge > STALE_LOCATION_AGE

    override val elevation: Distance
        get() = sensorSubsystem.lastKnownElevation

    override val elevationAge: Duration
        get() {
            if (isAltimeterOverridden()) {
                return Duration.ZERO
            }
            val lastUpdate = preferences.getInstant(SharedSensorCache.LAST_ALTITUDE_UPDATE_KEY)
                ?: Instant.fromEpochMilliseconds(0)
            return Clock.System.now() - lastUpdate
        }

    init {
        val locationChangedPrefs = listOf(
            SharedSensorCache.LAST_GPS_KEY,
            PreferenceKeys.latitudeOverride,
            PreferenceKeys.longitudeOverride,
            PreferenceKeys.autoLocation,
            PreferenceKeys.coordinateFormat
        )
        val elevationChangedPrefs = listOf(
            SharedSensorCache.LAST_ALTITUDE_UPDATE_KEY,
            PreferenceKeys.altitudeOverride,
            PreferenceKeys.altimeterCalibrationMode
        )
        preferences.onEachChange { key ->
            if (key in locationChangedPrefs) {
                ToolBroadcasts.broadcast(BROADCAST_LOCATION_CHANGED)
            }
            if (key in elevationChangedPrefs) {
                ToolBroadcasts.broadcast(BROADCAST_ELEVATION_CHANGED)
            }
        }
    }

    override suspend fun updateLocation() {
        sensorSubsystem.getLocation(SensorRefreshPolicy.Refresh)
    }

    private fun isAltimeterOverridden(): Boolean {
        val mode = userPrefs.altimeterMode
        val usesOverride = mode == UserPreferences.AltimeterMode.Override
        val usesGPS = mode == UserPreferences.AltimeterMode.GPSBarometer || mode == UserPreferences.AltimeterMode.GPS
        return usesOverride || (usesGPS && !sensorService.hasLocationPermission())
    }

    private fun isGPSOverridden(): Boolean {
        return userPrefs.gps.locationSource == GPSLocationSource.Manual || !sensorService.hasLocationPermission()
    }

    companion object {
        private val STALE_LOCATION_AGE = 2.minutes
        const val BROADCAST_LOCATION_CHANGED = "sensors-broadcast-location-changed"
        const val BROADCAST_ELEVATION_CHANGED = "sensors-broadcast-elevation-changed"
    }
}
