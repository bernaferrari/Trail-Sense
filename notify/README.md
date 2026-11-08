# Local notifications

`NotificationSender` is shared across platforms. Android callers provide a context, notification channel, and small icon; iOS callers use `notificationSender()` after requesting notification permission. On desktop, `notificationSender()` uses the system tray and accepts an optional AWT image for the tray icon.

Desktop notifications are transient and require a supported system tray. Each temporary tray icon is removed after 30 seconds or when its notification is cancelled. Whether cancelling also dismisses an already displayed notification depends on the desktop environment.
