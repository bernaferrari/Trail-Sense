package com.kylecorry.trail_sense.tools.weather.ui.fields

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import kotlin.time.Instant
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.TrailSenseColors

class LastUpdatedWeatherField(private val time: Instant?) : WeatherField {

    private val formatter = getAppService<FormatService>()

    override fun getListItem(context: Context): WeatherListItem? {
        time ?: return null

        val color = TrailSenseColors.current.onSurfaceVariant.toArgb()

        return WeatherListItem(
            8,
            context.getString(R.string.last_updated),
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
