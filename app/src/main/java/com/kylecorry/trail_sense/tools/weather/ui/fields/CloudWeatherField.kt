package com.kylecorry.trail_sense.tools.weather.ui.fields

import android.content.Context
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.ui.CloudDetailsModal

class CloudWeatherField(
    private val cloud: Reading<CloudGenus?>?
) : WeatherField {
    override fun getListItem(context: Context): WeatherListItem? {
        cloud ?: return null
        val cloudDetailsService = CloudDetailsService(context)
        val name = cloudDetailsService.getCloudName(cloud.value)

        return WeatherListItem(
            id = 7,
            title = context.getString(R.string.clouds),
            icon = R.drawable.cloudy,
            trailingText = name
        ) {
            CloudDetailsModal(context).show(cloud.value)
        }
    }
}
