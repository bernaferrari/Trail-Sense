package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import android.content.Context
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.commands.generic.ComposedCommand
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather

class SendWeatherAlertsCommand(private val context: Context) : Command<CurrentWeather> {

    override fun execute(value: CurrentWeather) {
        val command = ComposedCommand(
            WeatherAlertCommands.daily(context),
            WeatherAlertCommands.storm(context),
            WeatherAlertCommands.current(context)
        )
        command.execute(value)
    }

}
