package com.kylecorry.andromeda.notify

import java.awt.Color
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "andromeda-notification-cleanup").apply { isDaemon = true }
}

/** Sends transient desktop notifications using the system tray. */
public fun notificationSender(icon: Image = defaultNotificationIcon()): NotificationSender = object : NotificationSender {
    private val lock = Any()
    private val icons = mutableMapOf<Int, TrayIcon>()

    override fun send(id: Int, title: String, body: String) {
        check(SystemTray.isSupported()) { "System tray notifications are not supported in this environment" }
        val tray = SystemTray.getSystemTray()
        val notification = TrayIcon(icon).apply { isImageAutoSize = true }
        synchronized(lock) {
            icons.remove(id)?.let(tray::remove)
            tray.add(notification)
            icons[id] = notification
            try {
                notification.displayMessage(title, body, TrayIcon.MessageType.INFO)
            } catch (error: Exception) {
                icons.remove(id)
                tray.remove(notification)
                throw error
            }
        }
        cleanupExecutor.schedule({
            synchronized(lock) {
                if (icons[id] === notification) {
                    icons.remove(id)
                    tray.remove(notification)
                }
            }
        }, 30, TimeUnit.SECONDS)
    }

    override fun cancel(id: Int) {
        synchronized(lock) {
            val notification = icons.remove(id) ?: return
            SystemTray.getSystemTray().remove(notification)
        }
    }
}

private fun defaultNotificationIcon(): Image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB).apply {
    createGraphics().let { graphics ->
        try {
            graphics.color = Color(0x3f, 0x51, 0xb5)
            graphics.fillOval(1, 1, 14, 14)
            graphics.color = Color.WHITE
            graphics.fillOval(6, 6, 4, 4)
        } finally { graphics.dispose() }
    }
}
