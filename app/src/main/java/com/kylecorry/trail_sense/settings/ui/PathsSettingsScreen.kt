@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.notifications.NotificationChannelControl
import com.kylecorry.trail_sense.tools.paths.infrastructure.services.BacktrackService

/** The paths settings with Backtrack's Android notification channel. */
@Composable
fun PathsSettingsToolScreen() {
    val context = LocalContext.current
    val notifications = remember(context) { BacktrackNotificationChannel(context) }
    PathsSettingsScreen(notifications)
}

private class BacktrackNotificationChannel(private val context: Context) : NotificationChannelControl {
    override fun isBlocked(): Boolean = Notify.isChannelBlocked(context, BacktrackService.FOREGROUND_CHANNEL_ID)

    override fun openSettings() {
        context.startActivity(Intents.notificationSettings(context, BacktrackService.FOREGROUND_CHANNEL_ID))
    }
}
