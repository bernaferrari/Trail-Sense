package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class PowerPreferences(context: Context) : PreferenceRepo(context) {

    var areTilesEnabled by BooleanPreference(cache, PreferenceKeys.tilesEnabled, true)

    var autoLowPower by BooleanPreference(cache, PreferenceKeys.autoLowPower, false)

    var userEnabledLowPower by BooleanPreference(cache, "pref_auto_low_power_user", false)

    var startOnBoot by BooleanPreference(
        cache,
        PreferenceKeys.startOnBoot,
        true
    )
    var enableBatteryLog by BooleanPreference(
        cache,
        PreferenceKeys.batteryLogEnabled,
        true
    )

}
