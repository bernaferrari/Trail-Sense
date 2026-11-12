package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.time.Instant
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

class LastUpdatedWeatherField(private val time: Instant?) : WeatherField {

    private val formatter = getAppService<FormatService>()

    override fun getListItem(): WeatherListItem? {
        time ?: return null

        val color = TrailSenseColors.current.onSurfaceVariant.toArgb()

        return WeatherListItem(
            8,
            getAppService<AppStrings>().get(Res.string.last_updated),
            icon = TrailIcons.Update,
            iconTint = color,
            trailingText = formatter.formatDateTime(
                time,
                relative = true,
                abbreviateMonth = true
            )
        )
    }
}
