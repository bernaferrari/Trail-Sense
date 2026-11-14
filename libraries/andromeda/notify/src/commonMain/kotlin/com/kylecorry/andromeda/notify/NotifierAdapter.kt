package com.kylecorry.andromeda.notify

import com.mmk.kmpnotifier.notification.Notifier

/** Adapts an initialized KMPNotifier local notifier to the simple sender API. */
public fun Notifier.asNotificationSender(): NotificationSender = object : NotificationSender {
    override fun send(id: Int, title: String, body: String) = notify(id, title, body)
    override fun cancel(id: Int) = remove(id)
}
