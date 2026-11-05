package com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers

import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestBackgroundLocationCommand
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Turns on a sunrise or sunset alert service. With [effects], it then asks for exact alarms, restarts the service,
 * and offers background location (a snackbar with Settings) where the alerts need it.
 */
internal suspend fun enableAstronomyAlert(
    context: Context,
    serviceId: String,
    effects: ScreenEffects?,
    scope: CoroutineScope
) {
    val service = Tools.getService(context, serviceId)
    service?.enable()
    if (effects == null) {
        return
    }
    effects.requestPermission(AppPermission.ExactAlarms)
    scope.launch {
        service?.restart()
    }
    RequestBackgroundLocationCommand(
        context,
        BackgroundLocationSnackbarAlerter(context, effects, scope)
    ).execute()
}

/** The background location explanation as a snackbar whose Settings action asks for the permission. */
private class BackgroundLocationSnackbarAlerter(
    private val context: Context,
    private val effects: ScreenEffects,
    private val scope: CoroutineScope
) : IAlerter {
    override fun alert() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        scope.launch {
            val openSettings = effects.showSnackbar(
                context.getString(R.string.sunset_alerts_background_location_disclaimer),
                actionLabel = context.getString(R.string.settings),
                long = true
            )
            if (openSettings) {
                // The system permission screen supplies the detailed explanation.
                effects.requestPermission(AppPermission.BackgroundLocation)
            }
        }
    }
}
