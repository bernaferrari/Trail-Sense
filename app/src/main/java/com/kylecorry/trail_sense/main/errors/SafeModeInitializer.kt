package com.kylecorry.trail_sense.main.errors

import android.content.Context
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

/** Stores this start time and lets [SafeMode] judge it against the two before. */
fun SafeMode.initialize(context: Context) {
    val preferences = PreferencesSubsystem.getInstance(context).preferences
    val lastStartTime = preferences.getLong("cache_last_start_time") ?: 0L
    val lastMinus1StartTime = preferences.getLong("cache_last_minus_1_start_time") ?: 0L
    val now = System.currentTimeMillis()
    preferences.putLong("cache_last_start_time", now)
    preferences.putLong("cache_last_minus_1_start_time", lastStartTime)
    onAppStarted(lastStartTime, lastMinus1StartTime, now)
}
