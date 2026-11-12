package com.kylecorry.trail_sense.tools.weather.widgets

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolIds
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem

class WeatherToolWidgetView : SimpleToolWidgetView() {
    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val weather = getAppService<IWeatherSubsystem>()
        val formatter = FormatService.getInstance(context)
        val prefs = UserPreferences(context)
        val current = weather.getWeather()
        val temperature = current.observation?.temperature

        return ToolWidgetContent.Simple(
            title = context.getString(R.string.weather),
            subtitle = if (temperature != null) {
                context.getString(
                    R.string.dot_separated_pair,
                    formatter.formatWeather(current.prediction.primaryHourly),
                    formatter.formatTemperature(
                        temperature.convertTo(prefs.temperatureUnits)
                    )
                )
            } else {
                formatter.formatWeather(current.prediction.primaryHourly)
            },
            icon = formatter.getWeatherImage(current.prediction.primaryHourly),
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, ToolIds.WEATHER))
        )
    }
}
