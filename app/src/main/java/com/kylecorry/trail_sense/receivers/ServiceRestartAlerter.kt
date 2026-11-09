package com.kylecorry.trail_sense.receivers

import com.kylecorry.trail_sense.shared.notifications.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem

class ServiceRestartAlerter(private val context: Context) : IDismissibleAlerter {
    override fun alert() {

        val intent = MainActivity.pendingIntent(context)

        val notification = Notify.status(
            context,
            CHANNEL_SERVICE_RESTART,
            context.getString(R.string.restart_services_title),
            context.getString(R.string.restart_services_message),
            TrailIcons.Alert,
            group = NOTIFICATION_GROUP_SERVICE_RESTART,
            intent = intent,
            autoCancel = true,
            alertOnlyOnce = true,
        )
        DependencyRegistry.get<NotificationSubsystem>().send(NOTIFICATION_ID, notification)
    }

    override fun dismiss() {
        Notify.cancel(context, NOTIFICATION_ID)
    }

    companion object {
        private const val NOTIFICATION_ID = 23759823
        private const val NOTIFICATION_GROUP_SERVICE_RESTART = "trail_sense_service_restart"
        const val CHANNEL_SERVICE_RESTART = "service_restart"
    }
}
