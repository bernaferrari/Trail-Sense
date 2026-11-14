package com.kylecorry.andromeda.device

import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLHeading
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Instant

internal actual fun platformMagneticHeading(interval: Duration): Flow<Reading<Float>> = callbackFlow {
    if (!CLLocationManager.headingAvailable()) { close(); return@callbackFlow }
    val manager = CLLocationManager()
    var lastMillis: Long? = null
    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateHeading: CLHeading) {
            if (didUpdateHeading.headingAccuracy < 0) return
            val millis = (didUpdateHeading.timestamp.timeIntervalSince1970 * 1000).toLong()
            val previous = lastMillis
            if (previous != null && millis >= previous && millis - previous < interval.inWholeMilliseconds) return
            lastMillis = millis
            trySend(Reading(didUpdateHeading.magneticHeading.toFloat(), Instant.fromEpochMilliseconds(millis)))
        }
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            close(IllegalStateException(didFailWithError.localizedDescription))
        }
    }
    manager.delegate = delegate
    manager.requestWhenInUseAuthorization()
    manager.startUpdatingHeading()
    awaitClose { manager.stopUpdatingHeading(); manager.delegate = null; delegate.hashCode() }
}.flowOn(Dispatchers.Main)
