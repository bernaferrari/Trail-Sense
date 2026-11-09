package com.kylecorry.andromeda.preferences

import com.kylecorry.andromeda.preferences.Preferences

import com.kylecorry.andromeda.preferences.value

import kotlin.reflect.KProperty

class IntEnumPreference<T>(
    private val preferences: Preferences,
    private val name: String,
    private val mappings: Map<Int, T>,
    private val defaultValue: T,
    private val saveDefault: Boolean = false
) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val raw = preferences.getInt(name)
        if (raw == null && saveDefault) {
            val defaultKey = mappings.entries.firstOrNull { it.value == defaultValue }?.key ?: return defaultValue
            preferences.putInt(name, defaultKey)
            return defaultValue
        } else if (raw == null) {
            return defaultValue
        }
        return mappings[raw] ?: defaultValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val key = mappings.entries.firstOrNull { it.value == value }?.key ?: return
        preferences.putInt(name, key)
    }

}