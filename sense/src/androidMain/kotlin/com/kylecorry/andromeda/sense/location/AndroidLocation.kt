package com.kylecorry.andromeda.sense.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.kylecorry.andromeda.device.LocationReading
import com.kylecorry.sol.units.Coordinate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Instant

/** Android location reading with the monotonic timestamp supplied by the platform. */
public data class AndroidLocationReading(val value: LocationReading, val time: Instant, val elapsedRealtimeNanos: Long)

public fun androidLocationProviderAvailable(context: Context, provider: String): Boolean =
    (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)?.isProviderEnabled(provider) == true

/** Observes a specific Android provider and request configuration; location permission is required. */
@SuppressLint("MissingPermission")
public fun androidLocationReadings(
    context: Context,
    provider: String = LocationManager.GPS_PROVIDER,
    config: LocationRequestConfig = LocationRequestConfig(),
): Flow<AndroidLocationReading> = callbackFlow {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: throw UnsupportedOperationException("Location service is unavailable")
    fun emit(location: Location) {
        val value = LocationReading(
            coordinate = Coordinate(location.latitude, location.longitude),
            altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
            horizontalAccuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
            verticalAccuracyMeters = if (LocationCompat.hasVerticalAccuracy(location)) LocationCompat.getVerticalAccuracyMeters(location) else null,
            speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() },
            bearingDegrees = location.bearing.takeIf { location.hasBearing() },
            speedAccuracyMetersPerSecond = if (LocationCompat.hasSpeedAccuracy(location)) LocationCompat.getSpeedAccuracyMetersPerSecond(location) else null,
            bearingAccuracyDegrees = if (LocationCompat.hasBearingAccuracy(location)) LocationCompat.getBearingAccuracyDegrees(location) else null,
            mslAltitudeMeters = if (LocationCompat.hasMslAltitude(location)) LocationCompat.getMslAltitudeMeters(location) else null,
        )
        trySend(AndroidLocationReading(value, Instant.fromEpochMilliseconds(location.time), location.elapsedRealtimeNanos))
    }
    val request = LocationRequestCompat.Builder(config.frequency.inWholeMilliseconds)
        .setQuality(config.powerUsage.toLocationRequestQuality())
        .setMinUpdateDistanceMeters(config.minimumDistance.meters().value)
        .apply {
            config.minimumFrequency?.let { setMinUpdateIntervalMillis(it.inWholeMilliseconds) }
            config.maximumUpdates?.let { setMaxUpdates(it) }
            config.maximumUpdateDelay?.let { setMaxUpdateDelayMillis(it.inWholeMilliseconds) }
            config.maximumDuration?.let { setDurationMillis(it.inWholeMilliseconds) }
        }.build()
    val listener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) = emit(location)
    }
    manager.getLastKnownLocation(provider)?.let(::emit)
    LocationManagerCompat.requestLocationUpdates(manager, provider, request, listener, Looper.getMainLooper())
    awaitClose { LocationManagerCompat.removeUpdates(manager, listener) }
}
