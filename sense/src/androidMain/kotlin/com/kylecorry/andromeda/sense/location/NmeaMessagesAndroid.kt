package com.kylecorry.andromeda.sense.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Handler
import android.os.Looper
import com.ksensor.core.context.KSensorContext
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Instant

@SuppressLint("MissingPermission")
public actual fun nmeaMessages(): Flow<Reading<Nmea>> = callbackFlow {
    val manager = KSensorContext.get().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: throw UnsupportedOperationException("Location service is unavailable")
    val listener = OnNmeaMessageListener { message, timestamp ->
        if (message != null) trySend(Reading(Nmea(message), Instant.fromEpochMilliseconds(timestamp)))
    }
    check(manager.addNmeaListener(listener, Handler(Looper.getMainLooper()))) { "Unable to register NMEA listener" }
    awaitClose { manager.removeNmeaListener(listener) }
}
