package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import kotlin.time.Duration.Companion.minutes

import android.content.Context
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorScheduler
import kotlin.time.Duration
import com.kylecorry.trail_sense.shared.dialogs.dialog

class ChangeWeatherFrequencyCommand(
    private val context: Context,
    private val onChange: (Duration) -> Unit
) {
    private val prefs by lazy { UserPreferences(context) }

    fun execute(frequency: Duration) {
        if (frequency == Duration.ZERO) {
            return
        }

        prefs.weather.weatherUpdateFrequency = frequency
        onChange(frequency)
        WeatherMonitorScheduler.restart(context)
        if (frequency < 15.minutes) {
            Alerts.dialog(
                context,
                context.getString(R.string.battery_warning),
                context.getString(R.string.backtrack_battery_warning),
                cancelText = null
            )
        }
    }
}
