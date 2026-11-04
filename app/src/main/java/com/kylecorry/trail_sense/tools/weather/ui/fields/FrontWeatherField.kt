package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.sol.science.meteorology.WeatherFront
import com.kylecorry.trail_sense.R

class FrontWeatherField(private val front: WeatherFront?) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        front ?: return null

        val frontName: String
        val icon: androidx.compose.ui.graphics.vector.ImageVector
        when (front) {
            WeatherFront.Warm -> {
                frontName = context.getString(R.string.weather_warm_front)
                icon = TrailIcons.WarmWeatherFront
            }

            WeatherFront.Cold -> {
                frontName = context.getString(R.string.weather_cold_front)
                icon = TrailIcons.ColdWeatherFront
            }
        }

        return WeatherListItem(
            id = 4,
            title = context.getString(R.string.weather_front),
            icon = icon,
            trailingText = frontName
        )
    }
}
