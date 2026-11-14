package com.kylecorry.andromeda.notify

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

public fun notificationSender(context: Context, channelId: String, smallIcon: Int): NotificationSender =
    object : NotificationSender {
        @SuppressLint("MissingPermission")
        override fun send(id: Int, title: String, body: String) {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon).setContentTitle(title).setContentText(body).build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }
        override fun cancel(id: Int) = NotificationManagerCompat.from(context).cancel(id)
    }
