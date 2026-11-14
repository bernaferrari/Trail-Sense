package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsText

class CloudWeatherField(
    private val cloud: Reading<CloudGenus?>?,
    private val strings: AppStrings,
    private val onCloudClick: (CloudGenus?) -> Unit
) : WeatherField {
    override fun getListItem(): WeatherListItem? {
        cloud ?: return null
        val name = CloudDetailsText(strings).getCloudName(cloud.value)

        return WeatherListItem(
            id = 7,
            title = strings.get(Res.string.clouds),
            icon = TrailIcons.Cloudy,
            trailingText = name
        ) {
            onCloudClick(cloud.value)
        }
    }
}
