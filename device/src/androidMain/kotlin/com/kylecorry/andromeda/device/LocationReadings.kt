package com.kylecorry.andromeda.device

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Handler
import android.os.Looper
import androidx.core.location.LocationCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.ksensor.core.context.KSensorContext
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.andromeda.sense.location.Nmea
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Duration
import kotlin.time.Instant

@SuppressLint("MissingPermission")
internal actual fun platformLocation(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>> = callbackFlow {
    val manager = KSensorContext.get().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val request = LocationRequestCompat.Builder(interval.inWholeMilliseconds).setQuality(when (accuracy) {
        SensorAccuracy.PowerSave -> LocationRequestCompat.QUALITY_LOW_POWER
        SensorAccuracy.Balanced -> LocationRequestCompat.QUALITY_BALANCED_POWER_ACCURACY
        SensorAccuracy.HighPrecision -> LocationRequestCompat.QUALITY_HIGH_ACCURACY
    }).build()
    var nmeaAltitude: Pair<Long, Double>? = null
    val nmeaListener = OnNmeaMessageListener { message, timestamp ->
        Nmea(message).mslAltitude?.takeIf { it.isFinite() }?.let { nmeaAltitude = timestamp to it.toDouble() }
    }
    val listener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            val reading = LocationReading(
                coordinate = Coordinate(location.latitude, location.longitude),
                altitudeMeters = location.altitude.takeIf { location.hasAltitude() },
                horizontalAccuracyMeters = location.accuracy.takeIf { location.hasAccuracy() },
                verticalAccuracyMeters = if (LocationCompat.hasVerticalAccuracy(location)) LocationCompat.getVerticalAccuracyMeters(location) else null,
                speedMetersPerSecond = location.speed.takeIf { location.hasSpeed() },
                bearingDegrees = location.bearing.takeIf { location.hasBearing() },
                speedAccuracyMetersPerSecond = if (LocationCompat.hasSpeedAccuracy(location)) LocationCompat.getSpeedAccuracyMetersPerSecond(location) else null,
                bearingAccuracyDegrees = if (LocationCompat.hasBearingAccuracy(location)) LocationCompat.getBearingAccuracyDegrees(location) else null,
                mslAltitudeMeters = if (LocationCompat.hasMslAltitude(location)) LocationCompat.getMslAltitudeMeters(location)
                    else nmeaAltitude?.takeIf { kotlin.math.abs(location.time - it.first) <= 5_000L }?.second
            )
            trySend(Reading(reading, Instant.fromEpochMilliseconds(location.time)))
        }
    }
    val nmeaRegistered = try { manager.addNmeaListener(nmeaListener, Handler(Looper.getMainLooper())) } catch (_: SecurityException) { false }
    try {
        LocationManagerCompat.requestLocationUpdates(manager, LocationManager.GPS_PROVIDER, request, listener, Looper.getMainLooper())
    } catch (error: Exception) {
        if (nmeaRegistered) manager.removeNmeaListener(nmeaListener)
        close(error)
        return@callbackFlow
    }
    awaitClose {
        manager.removeUpdates(listener)
        if (nmeaRegistered) manager.removeNmeaListener(nmeaListener)
    }
}
