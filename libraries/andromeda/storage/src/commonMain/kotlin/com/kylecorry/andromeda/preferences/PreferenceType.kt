package com.kylecorry.andromeda.preferences

public enum class PreferenceType {
    Int,
    Boolean,
    String,
    Float,
    Long
}

public fun getPreferenceType(value: Any?): PreferenceType? {
    return when (value) {
        is String -> PreferenceType.String
        is Int -> PreferenceType.Int
        is Long -> PreferenceType.Long
        is Float -> PreferenceType.Float
        is Boolean -> PreferenceType.Boolean
        else -> null
    }
}