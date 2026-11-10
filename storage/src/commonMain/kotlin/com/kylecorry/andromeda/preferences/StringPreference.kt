package com.kylecorry.andromeda.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.reflect.KProperty

public class StringPreference(
    private val preferences: Preferences,
    private val name: String,
    private val defaultValue: String,
    private val saveDefault: Boolean = false
) {

    public operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val value = preferences.getString(name)
        if (value == null && saveDefault) {
            preferences.putString(name, defaultValue)
        }
        return value ?: defaultValue
    }

    public operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        preferences.putString(name, value)
    }

}