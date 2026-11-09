package com.kylecorry.andromeda.notify

/** Sends and cancels local notifications after the app has obtained notification permission. */
public interface NotificationSender {
    public fun send(id: Int, title: String, body: String)
    public fun cancel(id: Int)
}
