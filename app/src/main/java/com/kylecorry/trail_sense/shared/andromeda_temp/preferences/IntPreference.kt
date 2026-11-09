package com.kylecorry.trail_sense.shared.andromeda_temp.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.reflect.KProperty

class IntPreference(
    private val preferences: Preferences,
    private val name: String,
    private val defaultValue: Int,
    private val saveDefault: Boolean = false
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        val value = preferences.getInt(name)
        if (value == null && saveDefault) {
            preferences.putInt(name, defaultValue)
        }
        return value ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        preferences.putInt(name, value)
    }

}