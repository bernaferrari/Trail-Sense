package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor

// TODO: Build chart into this
class HumidityWeatherField(
    private val humidity: Float?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        humidity ?: return null
        if (!Sensors.hasHygrometer(context)) {
            return null
        }

        val formatter = FormatService.getInstance(context)
        val value = formatter.formatPercentage(humidity)

        return WeatherListItem(
            id = 6,
            title = context.getString(R.string.humidity),
            icon = R.drawable.ic_category_water,
            iconTint = AppColor.Blue.color,
            trailingText = value
        ) {
            onClick()
        }
    }
}
