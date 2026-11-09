package com.kylecorry.andromeda.widgets

import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** Connects shared shortcut requests to Android intents and icon resources. */
fun shortcutPinning(
    context: Context,
    icon: (Shortcut) -> Int,
    intent: (Shortcut) -> Intent,
): ShortcutPinning {
    val app = context.applicationContext
    return ShortcutPinning(
        supported = { ShortcutManagerCompat.isRequestPinShortcutSupported(app) },
        requestPin = { shortcut ->
            val extras = PersistableBundle().apply { shortcut.attributes.forEach { (key, value) -> putString(key, value) } }
            val info = ShortcutInfoCompat.Builder(app, shortcut.id)
                .setShortLabel(shortcut.label)
                .setIcon(IconCompat.createWithResource(app, icon(shortcut)))
                .setIntent(intent(shortcut))
                .setExtras(extras)
                .apply { shortcut.longLabel?.let(::setLongLabel) }
                .build()
            ShortcutManagerCompat.requestPinShortcut(app, info, null)
        },
    )
}
