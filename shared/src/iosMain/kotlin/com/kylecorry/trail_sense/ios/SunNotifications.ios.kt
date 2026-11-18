package com.kylecorry.trail_sense.ios

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Instant

internal suspend fun replaceSunNotifications(id: String, title: String, times: List<Instant>): Boolean {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val identifiers = List(28) { index -> "$id-$index" }
    if (times.isEmpty()) {
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
        return true
    }
    val authorized = suspendCancellableCoroutine { continuation ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionSound) { granted, _ ->
            if (continuation.isActive) continuation.resume(granted)
        }
    }
    if (!authorized) return false
    for ((index, time) in times.withIndex()) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setSound(UNNotificationSound.defaultSound)
        }
        val interval = ((time - Clock.System.now()).inWholeMilliseconds / 1000.0).coerceAtLeast(1.0)
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(interval, false)
        val request = UNNotificationRequest.requestWithIdentifier(identifiers[index], content, trigger)
        val added = suspendCancellableCoroutine { continuation ->
            center.addNotificationRequest(request) { error ->
                if (continuation.isActive) continuation.resume(error == null)
            }
        }
        if (!added) return false
    }
    center.removePendingNotificationRequestsWithIdentifiers(identifiers.drop(times.size))
    return true
}
