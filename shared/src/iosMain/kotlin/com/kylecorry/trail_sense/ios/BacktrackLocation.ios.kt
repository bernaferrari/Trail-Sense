package com.kylecorry.trail_sense.ios

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalForeignApi::class)
internal fun isBacktrackLocationBlocked(): Boolean = when (CLLocationManager.authorizationStatus()) {
    kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> false
    else -> true
}

@OptIn(ExperimentalForeignApi::class)
internal fun backtrackLocations() = callbackFlow {
    val manager = CLLocationManager()
    val earliestFix = Clock.System.now() - 30.seconds
    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            for (location in didUpdateLocations.filterIsInstance<CLLocation>()) {
                if (location.horizontalAccuracy < 0) continue
                val time = Instant.fromEpochMilliseconds((location.timestamp.timeIntervalSince1970 * 1000).toLong())
                if (time < earliestFix) continue
                trySend(
                    PathPoint(
                        id = 0,
                        pathId = 0,
                        coordinate = location.coordinate.useContents { Coordinate(latitude, longitude) },
                        elevation = location.altitude.toFloat().takeIf { location.verticalAccuracy >= 0 },
                        time = time
                    )
                )
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            if (didFailWithError.code == kCLErrorDenied) {
                close(IllegalStateException(didFailWithError.localizedDescription))
            }
        }
    }
    manager.delegate = delegate
    manager.activityType = CLActivityTypeFitness
    manager.desiredAccuracy = kCLLocationAccuracyBest
    manager.allowsBackgroundLocationUpdates = true
    manager.showsBackgroundLocationIndicator = true
    manager.pausesLocationUpdatesAutomatically = false
    manager.startUpdatingLocation()
    awaitClose {
        manager.stopUpdatingLocation()
        manager.delegate = null
        delegate.hashCode() // Core Location weakly references its delegate.
    }
}.flowOn(Dispatchers.Main)
