package com.kylecorry.andromeda.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.reflect.KProperty

class BooleanPreference(
    private val preferences: Preferences,
    private val name: String,
    private val defaultValue: Boolean,
    private val saveDefault: Boolean = false
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        val value = preferences.getBoolean(name)
        if (value == null && saveDefault) {
            preferences.putBoolean(name, defaultValue)
        }
        return value ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        preferences.putBoolean(name, value)
    }

}