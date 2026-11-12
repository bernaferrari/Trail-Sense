package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.shared.debugging.isDebug

class DebugPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    private var isTapMapToSetLocationEnabled by BooleanPreference(
        cache,
        TAP_MAP_TO_SET_LOCATION_KEY,
        false
    )

    var tapMapToSetLocation: Boolean
        get() = isDebug() && isTapMapToSetLocationEnabled
        set(value) {
            isTapMapToSetLocationEnabled = value
        }

    private var isCompassUnavailableEnabled by BooleanPreference(
        cache,
        COMPASS_UNAVAILABLE_KEY,
        false
    )

    var compassUnavailable: Boolean
        get() = isDebug() && isCompassUnavailableEnabled
        set(value) {
            isCompassUnavailableEnabled = value
        }

    private companion object {
        const val TAP_MAP_TO_SET_LOCATION_KEY = "pref_debug_tap_map_to_set_location"
        const val COMPASS_UNAVAILABLE_KEY = "pref_debug_compass_unavailable"
    }
}
