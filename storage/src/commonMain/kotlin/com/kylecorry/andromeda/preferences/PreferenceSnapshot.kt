package com.kylecorry.andromeda.preferences

/**
 * Reads every stored preference with its type.
 *
 * Settings backends do not expose stored types, so each value is probed with typed reads. Backends that
 * store values with their type (Android shared preferences, in-memory maps) report the exact type; backends
 * that store everything as text report [PreferenceType.String].
 */
fun Preferences.getAll(): Collection<Preference> {
    return keys.mapNotNull { key -> readTyped(key) }
}

/**
 * Writes the [preferences], optionally removing every preference that is not in the collection.
 */
fun Preferences.putAll(preferences: Collection<Preference>, clearOthers: Boolean = false) {
    if (clearOthers) {
        val keep = preferences.map { it.key }.toSet()
        keys.filterNot { it in keep }.forEach { remove(it) }
    }
    preferences.forEach { pref ->
        when (pref.type) {
            PreferenceType.Boolean -> putBoolean(pref.key, pref.value as Boolean)
            PreferenceType.String -> putString(pref.key, pref.value as String)
            PreferenceType.Int -> putInt(pref.key, pref.value as Int)
            PreferenceType.Long -> putLong(pref.key, pref.value as Long)
            PreferenceType.Float -> putFloat(pref.key, pref.value as Float)
        }
    }
}

private fun Preferences.readTyped(key: String): Preference? {
    val readers = listOf<Pair<PreferenceType, (String) -> Any?>>(
        PreferenceType.String to { settings.getStringOrNull(it) },
        PreferenceType.Boolean to { settings.getBooleanOrNull(it) },
        PreferenceType.Int to { settings.getIntOrNull(it) },
        PreferenceType.Long to { settings.getLongOrNull(it) },
        PreferenceType.Float to { settings.getFloatOrNull(it) },
    )
    for ((type, read) in readers) {
        val value = try {
            read(key)
        } catch (_: ClassCastException) {
            null
        }
        if (value != null) {
            return Preference(key, type, value)
        }
    }
    return null
}
