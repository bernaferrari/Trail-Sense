package com.kylecorry.andromeda.preferences

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

/** Opens the default Android preferences file, or a named file for separate app state. */
fun preferences(context: Context, name: String = "${context.packageName}_preferences"): Preferences = Preferences(
    SharedPreferencesSettings(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))
)
