package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor

// TODO: Build chart into this
class HumidityWeatherField(
    private val humidity: Float?,
    private val onClick: () -> Unit
) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        humidity ?: return null
        if (!getAppService<DeviceProfile>().hasHygrometer) {
            return null
        }

        val formatter = FormatService.getInstance()
        val value = formatter.formatPercentage(humidity)

        return WeatherListItem(
            id = 6,
            title = getAppService<AppStrings>().get(Res.string.humidity),
            icon = TrailIcons.CategoryWater,
            iconTint = AppColor.Blue.color,
            trailingText = value
        ) {
            onClick()
        }
    }
}
