package com.kylecorry.trail_sense.tools.pedometer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService

/** Pedometer settings on Android: permission and notification channel state, and the system settings pages. */
@Composable
fun PedometerSettingsToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val status = remember(appContext) {
        object : PedometerSystemStatus {
            override fun canRecognizeActivity() = Permissions.canRecognizeActivity(appContext)
            override fun isBackgroundLocationEnabled() = Permissions.isBackgroundLocationEnabled(appContext)
            override fun isNotificationChannelBlocked() =
                Notify.isChannelBlocked(appContext, StepCounterService.CHANNEL_ID)
        }
    }
    PedometerSettingsScreen(
        status = status,
        openAppSettings = { context.startActivity(Intents.appSettings(context)) },
        openNotificationSettings = {
            context.startActivity(Intents.notificationSettings(context, StepCounterService.CHANNEL_ID))
        }
    )
}
