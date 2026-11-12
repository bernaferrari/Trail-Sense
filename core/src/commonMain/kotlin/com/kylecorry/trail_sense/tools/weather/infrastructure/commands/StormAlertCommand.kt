package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.trail_sense.shared.alerts.IDismissibleAlerter
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.preferences.Flag
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherAlert
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences

class StormAlertCommand(
    private val justShownFlag: Flag,
    private val prefs: IWeatherPreferences,
    private val alerter: IDismissibleAlerter
) : Command<CurrentWeather> {

    override fun execute(value: CurrentWeather) {
        val sentAlert = justShownFlag.get()

        if (value.prediction.alerts.contains(WeatherAlert.Storm)) {
            val shouldSend = prefs.sendStormAlerts && prefs.shouldMonitorWeather
            if (shouldSend && !sentAlert) {
                alerter.alert()
                justShownFlag.set(true)
            }
        } else {
            alerter.dismiss()
            justShownFlag.set(false)
        }
    }
}
