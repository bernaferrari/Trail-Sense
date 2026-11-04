package com.kylecorry.trail_sense.tools.diagnostics.status

import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import com.kylecorry.andromeda.core.sensors.ISensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService

class SensorStatusBadgeProvider(
    private val sensor: ISensor,
    private val context: Context,
    private val icon: ImageVector
) : StatusBadgeProvider {

    private val formatter = FormatService.getInstance(context)
    private val isUnavailable = sensor is MockSensor

    override fun getBadge(): StatusBadge {
        if (isUnavailable) {
            return StatusBadge(
                context.getString(R.string.unavailable),
                CustomUiUtils.getQualityColor(Quality.Unknown),
                icon,
                isHealthy = false
            )
        }

        return StatusBadge(
            formatter.formatQuality(sensor.quality),
            CustomUiUtils.getQualityColor(sensor.quality),
            icon,
            isHealthy = sensor.quality == Quality.Good
        )
    }
}
