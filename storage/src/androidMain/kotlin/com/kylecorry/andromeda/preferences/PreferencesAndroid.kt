package com.kylecorry.andromeda.preferences

import android.content.Context
import android.content.SharedPreferences
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Opens the default Android preferences file, or a named file for separate app state.
 * @param commitChanges true to write changes synchronously (commit) instead of in the background (apply)
 */
public fun preferences(
    context: Context,
    name: String = "${context.packageName}_preferences",
    commitChanges: Boolean = false
): Preferences {
    val shared = context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)
    return Preferences(SharedPreferencesSettings(shared, commitChanges)) { onChange ->
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            key?.let(onChange)
        }
        shared.registerOnSharedPreferenceChangeListener(listener)
        return@Preferences { shared.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
