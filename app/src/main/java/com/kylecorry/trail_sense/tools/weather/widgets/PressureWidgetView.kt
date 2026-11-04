package com.kylecorry.trail_sense.tools.weather.widgets

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem

class PressureWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val weather = WeatherSubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val prefs = UserPreferences(context)
        val current = weather.getWeather()

        val pressure = current.observation?.pressure
        val subtitle = if (pressure != null) {
            val convertedPressure = pressure.convertTo(prefs.pressureUnits)
            formatter.formatPressure(
                convertedPressure,
                Units.getDecimalPlaces(convertedPressure.units)
            )
        } else {
            ""
        }

        val tendency = current.pressureTendency
        val iconRes = when {
            tendency.amount > 0 -> TrailIcons.ArrowUp
            tendency.amount < 0 -> TrailIcons.ArrowDown
            else -> TrailIcons.SteadyArrow
        }

        return ToolWidgetContent.Simple(
            title = context.getString(R.string.pressure),
            subtitle = subtitle,
            icon = iconRes,
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.WEATHER))
        )
    }
}
