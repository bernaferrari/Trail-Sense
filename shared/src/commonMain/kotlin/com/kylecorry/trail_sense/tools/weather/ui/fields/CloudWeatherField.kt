package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsText
import com.kylecorry.trail_sense.tools.clouds.ui.CloudDetailsModal

class CloudWeatherField(
    private val cloud: Reading<CloudGenus?>?
) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        cloud ?: return null
        val name = CloudDetailsText().getCloudName(cloud.value)

        return WeatherListItem(
            id = 7,
            title = getAppService<AppStrings>().get(Res.string.clouds),
            icon = TrailIcons.Cloudy,
            trailingText = name
        ) {
            CloudDetailsModal().show(cloud.value)
        }
    }
}
