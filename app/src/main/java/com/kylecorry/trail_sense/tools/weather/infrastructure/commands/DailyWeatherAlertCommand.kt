package com.kylecorry.trail_sense.tools.weather.infrastructure.commands

import com.kylecorry.trail_sense.shared.time.toLocalTime

import com.kylecorry.trail_sense.shared.time.toLocalDate

import android.content.Context
import com.kylecorry.andromeda.core.time.ZonedDateTimeProvider
import com.kylecorry.andromeda.core.time.SystemZonedDateTimeProvider
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.IValueAlerter
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.tools.weather.domain.CanSendDailyForecast
import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherPrediction
import com.kylecorry.trail_sense.tools.weather.infrastructure.IWeatherPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.DailyWeatherAlerter

class DailyWeatherAlertCommand(
    private val prefs: IWeatherPreferences,
    private val alerter: IValueAlerter<WeatherPrediction>,
    private val timeProvider: ZonedDateTimeProvider
) : Command<CurrentWeather> {

    override fun execute(value: CurrentWeather) {
        if (!prefs.shouldShowDailyWeatherNotification || !prefs.shouldMonitorWeather) {
            return
        }

        val now = timeProvider.getTime()
        val today = now.instant.toLocalDate(now.timeZone)

        if (today == prefs.dailyWeatherLastSent) {
            return
        }

        if (!CanSendDailyForecast(prefs.dailyForecastTime).isSatisfiedBy(now.instant.toLocalTime(now.timeZone))) {
            return
        }

        prefs.dailyWeatherLastSent = today
        alerter.alert(value.prediction)
    }

    companion object {
        fun create(context: Context): DailyWeatherAlertCommand {
            val prefs = UserPreferences(context).weather
            return DailyWeatherAlertCommand(
                prefs,
                DailyWeatherAlerter(context, FormatService.getInstance(context), prefs),
                SystemZonedDateTimeProvider()
            )
        }
    }
}
