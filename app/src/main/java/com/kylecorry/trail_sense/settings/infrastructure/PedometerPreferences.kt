package com.kylecorry.trail_sense.settings.infrastructure

import kotlin.time.Duration.Companion.days

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.BooleanPreference
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.StringEnumPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo
import com.kylecorry.trail_sense.tools.pedometer.domain.AveragePaceTimeMode
import kotlin.time.Duration

class PedometerPreferences(context: Context) : PreferenceRepo(context), IPedometerPreferences {
    override var isEnabled by BooleanPreference(
        cache,
        getString(R.string.pref_pedometer_enabled),
        false
    )

    override val resetDaily by BooleanPreference(
        cache,
        getString(R.string.pref_odometer_reset_daily),
        true
    )

    override var strideLength: Distance
        get() {
            val raw = cache.getFloat(getString(R.string.pref_stride_length)) ?: 0.7f
            return Distance.meters(raw)
        }
        set(value) {
            cache.putFloat(getString(R.string.pref_stride_length), value.meters().value)
        }

    override var alertDistance: Distance?
        get() {
            val raw = cache.getFloat(getString(R.string.pref_distance_alert)) ?: return null
            return Distance.meters(raw)
        }
        set(value) {
            if (value == null) {
                cache.remove(getString(R.string.pref_distance_alert))
                return
            }
            cache.putFloat(getString(R.string.pref_distance_alert), value.meters().value)
        }

    override val useAlarmForDistanceAlert by BooleanPreference(
        cache,
        getString(R.string.pref_pedometer_use_alarm_for_distance_alert),
        false
    )

    override var averagePaceTimeMode by StringEnumPreference(
        cache,
        getString(R.string.pref_pedometer_average_pace_time),
        AveragePaceTimeMode.entries.associateBy { it.id.toString() },
        AveragePaceTimeMode.Active
    )

    override var stepHistory: Duration
        get() {
            val days = cache.getInt(getString(R.string.pref_pedometer_history_days)) ?: 30
            return (days.toLong()).days
        }
        set(value) {
            val days = value.inWholeDays.toInt()
            cache.putInt(getString(R.string.pref_pedometer_history_days), if (days > 0) days else 1)
        }
}
