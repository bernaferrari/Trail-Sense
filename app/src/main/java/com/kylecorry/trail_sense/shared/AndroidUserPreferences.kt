package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.trail_sense.shared.device.AndroidDeviceProfile
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

/** The app's settings, backed by the app's shared preferences and this device's capabilities. */
fun UserPreferences(context: Context): UserPreferences {
    return UserPreferences(
        PreferencesSubsystem.getInstance(context).preferences,
        AndroidDeviceProfile(context)
    )
}
