package com.kylecorry.trail_sense.shared.preferences

import android.content.Context
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.preferences

class PreferencesSubsystem private constructor(context: Context) {
    val preferences: Preferences = preferences(context)

    companion object {
        private var instance: PreferencesSubsystem? = null
        fun getInstance(context: Context): PreferencesSubsystem {
            if (instance == null) {
                instance = PreferencesSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}
