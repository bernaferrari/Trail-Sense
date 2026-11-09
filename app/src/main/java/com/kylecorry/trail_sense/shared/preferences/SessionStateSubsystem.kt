package com.kylecorry.trail_sense.shared.preferences

import android.content.Context
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.preferences

class SessionStateSubsystem private constructor(context: Context) {
    val preferences: Preferences = preferences(context, SESSION_STATE)

    companion object {
        private const val SESSION_STATE = "session_state"
        private var instance: SessionStateSubsystem? = null
        fun getInstance(context: Context): SessionStateSubsystem {
            if (instance == null) {
                instance = SessionStateSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }
}
