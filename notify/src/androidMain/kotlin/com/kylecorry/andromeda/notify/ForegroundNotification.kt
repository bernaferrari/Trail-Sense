package com.kylecorry.andromeda.notify

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

/** Builds an ongoing notification suitable for an Android foreground service. */
public fun foregroundNotification(
    context: Context,
    channelId: String,
    icon: Int,
    title: String,
    body: String? = null,
    contentIntent: PendingIntent? = null,
    actions: List<NotificationCompat.Action> = emptyList(),
    immediate: Boolean = false,
): Notification = NotificationCompat.Builder(context, channelId)
    .setSmallIcon(icon)
    .setContentTitle(title)
    .setContentText(body)
    .setContentIntent(contentIntent)
    .setOngoing(true)
    .setSilent(true)
    .setOnlyAlertOnce(true)
    .setPriority(NotificationCompat.PRIORITY_LOW)
    .setCategory(NotificationCompat.CATEGORY_SERVICE)
    .apply {
        if (immediate) setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        actions.forEach(::addAction)
    }
    .build()
