package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.settings.PreferenceKeys

class ClinometerPreferences(private val cache: Preferences) : IClinometerPreferences {


    override var lockWithVolumeButtons by BooleanPreference(
        cache,
        PreferenceKeys.clinometerLockWithVolumeButtons,
        false
    )
    override var baselineDistance: Distance?
        get() {
            val baseline =
                cache.getFloat(PreferenceKeys.clinometerBaselineDistance)
                    ?: return null
            val baselineUnitId =
                cache.getInt(PreferenceKeys.clinometerBaselineDistanceUnits)
                    ?: return null
            val units = DistanceUnits.entries.firstOrNull { baselineUnitId == it.id }
                ?: DistanceUnits.Meters
            return Distance.from(baseline, units)
        }
        set(value) {
            if (value == null) {
                cache.remove(PreferenceKeys.clinometerBaselineDistance)
                cache.remove(PreferenceKeys.clinometerBaselineDistanceUnits)
            } else {
                cache.putFloat(
                    PreferenceKeys.clinometerBaselineDistance,
                    value.value
                )
                cache.putInt(
                    PreferenceKeys.clinometerBaselineDistanceUnits,
                    value.units.id
                )
            }
        }
}
