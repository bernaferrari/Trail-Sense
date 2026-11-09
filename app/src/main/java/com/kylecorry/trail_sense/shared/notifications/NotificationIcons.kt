package com.kylecorry.trail_sense.shared.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.NotificationCompat
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.icons.toBitmap
import com.kylecorry.trail_sense.shared.icons.toIconCompat

/*
 * Andromeda's Notify builds notifications from a drawable resource id, but Trail Sense's icons are Compose vectors.
 * These overloads take the vector instead: Notify builds the notification around a framework placeholder, then the
 * small icon (white on transparent, as the status bar expects) and the optional large icon are swapped for the
 * vector drawn as a bitmap. Parameters and defaults mirror Notify's.
 */

private const val PLACEHOLDER_ICON = android.R.drawable.ic_dialog_info

fun Notify.alert(
    context: Context,
    channel: String,
    title: String,
    contents: String?,
    icon: ImageVector,
    autoCancel: Boolean = false,
    alertOnlyOnce: Boolean = false,
    showBigIcon: Boolean = false,
    group: String? = null,
    intent: PendingIntent? = null,
    actions: List<NotificationCompat.Action> = emptyList(),
    isAlarm: Boolean = false,
    mute: Boolean = false,
    category: String? = null
): Notification = alert(
    context,
    channel,
    title,
    contents,
    PLACEHOLDER_ICON,
    autoCancel = autoCancel,
    alertOnlyOnce = alertOnlyOnce,
    showBigIcon = false,
    group = group,
    intent = intent,
    actions = actions,
    isAlarm = isAlarm,
    mute = mute,
    category = category
).withIcon(context, icon, showBigIcon)

fun Notify.status(
    context: Context,
    channel: String,
    title: String,
    contents: String?,
    icon: ImageVector,
    autoCancel: Boolean = false,
    alertOnlyOnce: Boolean = false,
    showBigIcon: Boolean = false,
    group: String? = null,
    intent: PendingIntent? = null,
    actions: List<NotificationCompat.Action> = emptyList(),
    category: String? = "status"
): Notification = status(
    context,
    channel,
    title,
    contents,
    PLACEHOLDER_ICON,
    autoCancel = autoCancel,
    alertOnlyOnce = alertOnlyOnce,
    showBigIcon = false,
    group = group,
    intent = intent,
    actions = actions,
    category = category
).withIcon(context, icon, showBigIcon)

fun Notify.persistent(
    context: Context,
    channel: String,
    title: String,
    contents: String?,
    icon: ImageVector,
    autoCancel: Boolean = false,
    alertOnlyOnce: Boolean = true,
    showBigIcon: Boolean = false,
    group: String? = null,
    intent: PendingIntent? = null,
    actions: List<NotificationCompat.Action> = emptyList(),
    showForegroundImmediate: Boolean = false,
    category: String? = "service"
): Notification = persistent(
    context,
    channel,
    title,
    contents,
    PLACEHOLDER_ICON,
    autoCancel = autoCancel,
    alertOnlyOnce = alertOnlyOnce,
    showBigIcon = false,
    group = group,
    intent = intent,
    actions = actions,
    showForegroundImmediate = showForegroundImmediate,
    category = category
).withIcon(context, icon, showBigIcon)

/** A notification action whose icon is drawn from [icon]. */
fun Notify.action(
    context: Context,
    name: String,
    intent: PendingIntent,
    icon: ImageVector
): NotificationCompat.Action = action(name, intent, icon.toIconCompat(context, 24f, Color.WHITE))

/** This notification with its small icon (and, when [showBigIcon], its large icon) drawn from [icon]. */
fun Notification.withIcon(context: Context, icon: ImageVector, showBigIcon: Boolean = false): Notification {
    val builder = Notification.Builder.recoverBuilder(context, this)
        .setSmallIcon(Icon.createWithBitmap(icon.toBitmap(context, 24f, Color.WHITE)))
    if (showBigIcon) {
        builder.setLargeIcon(icon.toBitmap(context, 24f))
    }
    return builder.build()
}
