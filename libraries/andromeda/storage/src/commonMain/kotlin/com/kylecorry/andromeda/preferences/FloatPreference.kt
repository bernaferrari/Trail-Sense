package com.kylecorry.andromeda.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.reflect.KProperty

public class FloatPreference(
    private val preferences: Preferences,
    private val name: String,
    private val defaultValue: Float,
    private val saveDefault: Boolean = false
) {

    public operator fun getValue(thisRef: Any?, property: KProperty<*>): Float {
        val value = preferences.getFloat(name)
        if (value == null && saveDefault) {
            preferences.putFloat(name, defaultValue)
        }
        return value ?: defaultValue
    }

    public operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        preferences.putFloat(name, value)
    }

}