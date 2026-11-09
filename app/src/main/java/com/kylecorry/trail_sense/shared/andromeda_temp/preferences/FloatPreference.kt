package com.kylecorry.trail_sense.shared.andromeda_temp.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.reflect.KProperty

class FloatPreference(
    private val preferences: Preferences,
    private val name: String,
    private val defaultValue: Float,
    private val saveDefault: Boolean = false
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        val value = preferences.getFloat(name)
        if (value == null && saveDefault) {
            preferences.putFloat(name, defaultValue)
        }
        return value ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        preferences.putFloat(name, value)
    }

}