package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherMonitorScheduler
import java.time.Duration

class ChangeWeatherFrequencyCommand(
    private val context: Context,
    private val onChange: (Duration) -> Unit
) {
    private val prefs by lazy { UserPreferences(context) }

    fun execute(frequency: Duration) {
        if (frequency.isZero) {
            return
        }

        prefs.weather.weatherUpdateFrequency = frequency
        onChange(frequency)
        WeatherMonitorScheduler.restart(context)
        if (frequency < Duration.ofMinutes(15)) {
            Alerts.dialog(
                context,
                context.getString(R.string.battery_warning),
                context.getString(R.string.backtrack_battery_warning),
                cancelText = null
            )
        }
    }
}
