package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences

class CurrentWeatherAlertCommand(
    private val prefs: IWeatherPreferences,
    private val alerter: IValueAlerter<CurrentWeather>
) : Command<CurrentWeather> {
    override fun execute(value: CurrentWeather) {
        if (prefs.shouldMonitorWeather) {
            alerter.alert(value)
        }
    }
}
