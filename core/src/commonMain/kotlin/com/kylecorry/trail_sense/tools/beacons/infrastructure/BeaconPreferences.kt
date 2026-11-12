package com.kylecorry.trail_sense.tools.beacons.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.preferences.IntEnumPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.IBeaconPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.BeaconSortMethod

class BeaconPreferences(private val cache: Preferences) : IBeaconPreferences {


    override val showLastSignalBeacon: Boolean
        get() = cache.getBoolean(PreferenceKeys.showLastSignalBeacon) ?: true

    override var beaconSort: BeaconSortMethod by IntEnumPreference(
        cache,
        PreferenceKeys.beaconSort,
        BeaconSortMethod.values().associateBy { it.id.toInt() },
        BeaconSortMethod.Closest
    )

    override var defaultBeaconColor: AppColor
        get() {
            val id = cache.getLong(PreferenceKeys.beaconDefaultColor)
            return AppColor.entries.firstOrNull { it.id == id } ?: AppColor.Orange
        }
        set(value) {
            cache.putLong(PreferenceKeys.beaconDefaultColor, value.id)
        }
}
