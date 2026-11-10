package com.kylecorry.andromeda.sense.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.location.LocationManager
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Duration

/**
 * Registers an Android proximity alert, which can deliver a pending intent in the background.
 * Fine-location permission and any applicable background-location permission are required.
 * Close the returned handle to unregister the alert.
 */
@SuppressLint("MissingPermission")
public fun registerProximityAlert(
    context: Context,
    center: Coordinate,
    radius: Distance,
    action: PendingIntent,
    expiration: Duration? = null,
): AutoCloseable {
    val meters = radius.meters().value
    require(meters.isFinite() && meters > 0)
    require(expiration == null || (expiration.isFinite() && expiration >= Duration.ZERO))
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: throw UnsupportedOperationException("Location service is unavailable")
    @Suppress("DEPRECATION")
    manager.addProximityAlert(center.latitude, center.longitude, meters, expiration?.inWholeMilliseconds ?: -1L, action)
    return AutoCloseable {
        @Suppress("DEPRECATION")
        manager.removeProximityAlert(action)
    }
}
