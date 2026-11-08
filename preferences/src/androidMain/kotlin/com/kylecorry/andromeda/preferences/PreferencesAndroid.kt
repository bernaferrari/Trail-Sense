package com.kylecorry.andromeda.preferences

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings

/** Uses the app's default Android preferences file. */
fun preferences(context: Context): Preferences = Preferences(
    SharedPreferencesSettings(context.applicationContext.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE))
)
