package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.time.Duration.Companion.days

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import kotlin.time.Duration

class PedometerPreferences(preferences: Preferences) : PreferenceRepo(preferences), IPedometerPreferences {
    override var isEnabled by BooleanPreference(
        cache,
        PreferenceKeys.pedometerEnabled,
        false
    )

    override val resetDaily by BooleanPreference(
        cache,
        PreferenceKeys.odometerResetDaily,
        true
    )

    override var strideLength: Distance
        get() {
            val raw = cache.getFloat(PreferenceKeys.strideLength) ?: 0.7f
            return Distance.meters(raw)
        }
        set(value) {
            cache.putFloat(PreferenceKeys.strideLength, value.meters().value)
        }

    override var alertDistance: Distance?
        get() {
            val raw = cache.getFloat(PreferenceKeys.distanceAlert) ?: return null
            return Distance.meters(raw)
        }
        set(value) {
            if (value == null) {
                cache.remove(PreferenceKeys.distanceAlert)
                return
            }
            cache.putFloat(PreferenceKeys.distanceAlert, value.meters().value)
        }

    override val useAlarmForDistanceAlert by BooleanPreference(
        cache,
        PreferenceKeys.pedometerUseAlarmForDistanceAlert,
        false
    )

    override var averagePaceTimeMode by StringEnumPreference(
        cache,
        PreferenceKeys.pedometerAveragePaceTime,
        AveragePaceTimeMode.entries.associateBy { it.id.toString() },
        AveragePaceTimeMode.Active
    )

    override var stepHistory: Duration
        get() {
            val days = cache.getInt(PreferenceKeys.pedometerHistoryDays) ?: 30
            return (days.toLong()).days
        }
        set(value) {
            val days = value.inWholeDays.toInt()
            cache.putInt(PreferenceKeys.pedometerHistoryDays, if (days > 0) days else 1)
        }
}
