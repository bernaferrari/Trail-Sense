package com.kylecorry.andromeda.device

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Instant

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformLocation(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>> = callbackFlow {
    val manager = CLLocationManager()
    manager.desiredAccuracy = when (accuracy) {
        SensorAccuracy.PowerSave -> kCLLocationAccuracyKilometer
        SensorAccuracy.Balanced -> kCLLocationAccuracyHundredMeters
        SensorAccuracy.HighPrecision -> kCLLocationAccuracyBest
    }
    var lastReadingMillis: Long? = null
    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            val millis = (location.timestamp.timeIntervalSince1970 * 1000).toLong()
            val last = lastReadingMillis
            if (last != null && millis >= last && millis - last < interval.inWholeMilliseconds) return
            lastReadingMillis = millis
            val coordinate = location.coordinate.useContents { Coordinate(latitude, longitude) }
            val reading = LocationReading(
                coordinate = coordinate,
                altitudeMeters = location.altitude.takeIf { location.verticalAccuracy >= 0 },
                horizontalAccuracyMeters = location.horizontalAccuracy.toFloat().takeIf { it >= 0 },
                verticalAccuracyMeters = location.verticalAccuracy.toFloat().takeIf { it >= 0 },
                speedMetersPerSecond = location.speed.toFloat().takeIf { it >= 0 },
                bearingDegrees = location.course.toFloat().takeIf { it >= 0 },
                speedAccuracyMetersPerSecond = location.speedAccuracy.toFloat().takeIf { it >= 0 },
                bearingAccuracyDegrees = location.courseAccuracy.toFloat().takeIf { it >= 0 },
                mslAltitudeMeters = location.altitude.takeIf { location.verticalAccuracy >= 0 }
            )
            trySend(Reading(reading, Instant.fromEpochMilliseconds(millis)))
        }
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            close(IllegalStateException(didFailWithError.localizedDescription))
        }
    }
    manager.delegate = delegate
    manager.requestWhenInUseAuthorization()
    manager.startUpdatingLocation()
    awaitClose {
        manager.stopUpdatingLocation()
        manager.delegate = null
        delegate.hashCode() // Keep the weak Core Location delegate alive until collection ends.
    }
}.flowOn(Dispatchers.Main)
