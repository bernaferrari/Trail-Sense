package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Instant

class LastUpdatedWeatherField(private val time: Instant?) : WeatherField {

    private val formatter = getAppService<FormatService>()

    override fun getListItem(context: Context): WeatherListItem? {
        time ?: return null

        val color = Resources.androidTextColorSecondary(context)

        return WeatherListItem(
            8,
            context.getString(R.string.last_updated),
            icon = TrailIcons.Update,
            iconTint = color,
            trailingText = formatter.formatDateTime(
                time.toZonedDateTime(),
                relative = true,
                abbreviateMonth = true
            )
        )
    }
}
