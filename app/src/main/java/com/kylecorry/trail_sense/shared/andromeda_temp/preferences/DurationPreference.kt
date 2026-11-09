package com.kylecorry.trail_sense.shared.andromeda_temp.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.time.Duration
import kotlin.reflect.KProperty

class DurationPreference(
    private val preferences: Preferences,
    private val name: String,
    private val defaultValue: Duration,
    private val saveDefault: Boolean = false
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Duration {
        val value = preferences.getDuration(name)
        if (value == null && saveDefault) {
            preferences.putDuration(name, defaultValue)
        }
        return value ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Duration) {
        preferences.putDuration(name, value)
    }

}