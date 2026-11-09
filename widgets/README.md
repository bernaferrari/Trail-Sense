# Widgets and shortcuts

Use `WidgetManager.update()` from shared code to request a widget refresh. `isInstalled()` returns `null` when the platform cannot report installation state.

On Android, create a manager with `widgetManager(context, glanceWidget)`. On iOS and desktop, provide the update callback from the host. For iOS widgets, connect that callback to the host's WidgetKit integration.

`ShortcutPinning` exposes shortcut requests to shared code. The Android `shortcutPinning` adapter maps each request to an icon resource and an intent. A successful request means the launcher accepted the request; it does not mean the user pinned the shortcut. Other platforms require a host-provided implementation and may not support pinning.
