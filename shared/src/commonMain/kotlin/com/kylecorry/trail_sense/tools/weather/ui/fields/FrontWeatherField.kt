package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.sol.science.meteorology.WeatherFront

class FrontWeatherField(private val front: WeatherFront?) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        front ?: return null

        val frontName: String
        val icon: androidx.compose.ui.graphics.vector.ImageVector
        when (front) {
            WeatherFront.Warm -> {
                frontName = getAppService<AppStrings>().get(Res.string.weather_warm_front)
                icon = TrailIcons.WarmWeatherFront
            }

            WeatherFront.Cold -> {
                frontName = getAppService<AppStrings>().get(Res.string.weather_cold_front)
                icon = TrailIcons.ColdWeatherFront
            }
        }

        return WeatherListItem(
            id = 4,
            title = getAppService<AppStrings>().get(Res.string.weather_front),
            icon = icon,
            trailingText = frontName
        )
    }
}
