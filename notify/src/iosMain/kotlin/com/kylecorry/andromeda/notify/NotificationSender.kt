package com.kylecorry.andromeda.notify

import platform.UserNotifications.*

fun notificationSender(): NotificationSender = object : NotificationSender {
    private val center = UNUserNotificationCenter.currentNotificationCenter()
    override fun send(id: Int, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
        }
        val request = UNNotificationRequest.requestWithIdentifier(id.toString(), content, null)
        center.addNotificationRequest(request, null)
    }
    override fun cancel(id: Int) {
        val identifiers = listOf(id.toString())
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
        center.removeDeliveredNotificationsWithIdentifiers(identifiers)
    }
}
