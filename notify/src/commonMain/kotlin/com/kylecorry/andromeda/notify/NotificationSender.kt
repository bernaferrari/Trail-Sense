package com.kylecorry.andromeda.notify

/** Sends and cancels local notifications after the app has obtained notification permission. */
interface NotificationSender {
    fun send(id: Int, title: String, body: String)
    fun cancel(id: Int)
}
