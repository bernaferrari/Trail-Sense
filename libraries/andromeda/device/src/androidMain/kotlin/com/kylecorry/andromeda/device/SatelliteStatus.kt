package com.kylecorry.andromeda.device

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.location.GnssStatusCompat
import androidx.core.location.LocationManagerCompat
import com.ksensor.core.context.KSensorContext
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.andromeda.sense.location.fromStatus
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Clock

@SuppressLint("MissingPermission")
internal actual fun platformSatelliteStatus(): Flow<Reading<List<Satellite>>> = callbackFlow {
    val manager = KSensorContext.get().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val listener = object : GnssStatusCompat.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatusCompat) {
            trySend(Reading(Satellite.fromStatus(status), Clock.System.now()))
        }
    }
    val registered = LocationManagerCompat.registerGnssStatusCallback(manager, listener, Handler(Looper.getMainLooper()))
    check(registered) { "Unable to register GNSS status callback" }
    awaitClose { LocationManagerCompat.unregisterGnssStatusCallback(manager, listener) }
}
