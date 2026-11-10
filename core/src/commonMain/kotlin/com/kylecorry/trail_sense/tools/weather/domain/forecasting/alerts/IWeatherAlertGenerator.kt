package com.kylecorry.trail_sense.tools.weather.domain.forecasting.alerts

import com.kylecorry.trail_sense.tools.weather.domain.CurrentWeather
import com.kylecorry.trail_sense.tools.weather.domain.WeatherAlert

interface IWeatherAlertGenerator {
    fun getAlerts(weather: CurrentWeather): List<WeatherAlert>
}
