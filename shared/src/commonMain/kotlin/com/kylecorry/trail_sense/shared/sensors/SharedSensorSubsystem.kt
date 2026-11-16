package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.math.MathExtensions.real
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import kotlin.time.Duration

/**
 * Shared [ISensorSubsystem] implementation backed by [SensorService], [Preferences], and [UserPreferences].
 */
class SharedSensorSubsystem(
    private val sensorService: SensorService,
    private val preferences: Preferences,
    private val userPrefs: UserPreferences,
    private val locationAvailableCheck: () -> Boolean = { true }
) : ISensorSubsystem {

    private val timezoneGPS by lazy { SharedTimezoneGPS() }

    override val lastKnownLocation: Coordinate
        get() = when (
            selectGpsSource(
                userPrefs,
                sensorService.hasLocationPermission(),
                locationAvailableCheck(),
                useCache = true
            )
        ) {
            SharedGpsSource.Override -> userPrefs.gps.locationOverride
            SharedGpsSource.Timezone -> timezoneGPS.location
            else -> {
                val cached = SharedSensorCache.readGps(preferences)
                val override = userPrefs.gps.locationOverride
                Coordinate(
                    cached?.latitude ?: override.latitude,
                    cached?.longitude ?: override.longitude
                )
            }
        }

    override val lastKnownLocationAccuracy: Distance?
        get() = when (
            selectGpsSource(
                userPrefs,
                sensorService.hasLocationPermission(),
                locationAvailableCheck(),
                useCache = true
            )
        ) {
            SharedGpsSource.Override, SharedGpsSource.Timezone -> null
            else -> SharedSensorCache.readGps(preferences)?.horizontalAccuracy?.let { Distance.meters(it) }
        }

    override val lastKnownElevation: Distance
        get() {
            val altimeter = when (userPrefs.altimeterMode) {
                UserPreferences.AltimeterMode.Override -> SharedOverrideAltimeter(userPrefs)
                UserPreferences.AltimeterMode.Barometer,
                UserPreferences.AltimeterMode.GPSBarometer,
                UserPreferences.AltimeterMode.DigitalElevationModel,
                UserPreferences.AltimeterMode.DigitalElevationModelBarometer ->
                    SharedCachedAltimeter(preferences, userPrefs)
                UserPreferences.AltimeterMode.GPS ->
                    if (sensorService.hasLocationPermission()) {
                        SharedCachedAltimeter(preferences, userPrefs)
                    } else {
                        SharedOverrideAltimeter(userPrefs)
                    }
            }
            return Distance.meters(altimeter.altitude.real(0f))
        }

    override suspend fun getLocation(
        preferredPolicy: SensorRefreshPolicy,
        timeout: Duration
    ): Coordinate {
        if (preferredPolicy == SensorRefreshPolicy.Cache) {
            return lastKnownLocation
        }
        val gps = sensorService.getGPS(SensorService.SINGLE_FIX_GPS_FREQUENCY, tag = "SharedSensorSubsystem.getLocation")
        readAll(
            listOf(gps),
            timeout,
            onlyIfInvalid = preferredPolicy == SensorRefreshPolicy.RefreshIfInvalid
        )
        return gps.location
    }

    override suspend fun getElevation(
        preferredPolicy: SensorRefreshPolicy,
        timeout: Duration
    ): Distance {
        if (preferredPolicy == SensorRefreshPolicy.Cache) {
            return lastKnownElevation
        }
        val altimeter = sensorService.getAltimeter(
            frequency = SensorService.SINGLE_FIX_GPS_FREQUENCY,
            tag = "SharedSensorSubsystem.getElevation"
        )
        readAll(
            listOf(altimeter),
            timeout,
            onlyIfInvalid = preferredPolicy == SensorRefreshPolicy.RefreshIfInvalid
        )
        return Distance.meters(altimeter.altitude)
    }

    override suspend fun getLocationAndElevation(
        preferredPolicy: SensorRefreshPolicy,
        timeout: Duration
    ): Pair<Coordinate, Distance> {
        if (preferredPolicy == SensorRefreshPolicy.Cache) {
            return lastKnownLocation to lastKnownElevation
        }
        val gps = sensorService.getGPS(
            SensorService.SINGLE_FIX_GPS_FREQUENCY,
            tag = "SharedSensorSubsystem.getLocationAndElevation"
        )
        val altimeter = sensorService.getAltimeter(gps = gps)
        readAll(
            listOf(gps, altimeter),
            timeout,
            onlyIfInvalid = preferredPolicy == SensorRefreshPolicy.RefreshIfInvalid
        )
        return gps.location to Distance.meters(altimeter.altitude)
    }
}
