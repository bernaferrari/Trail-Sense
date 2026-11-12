package com.kylecorry.trail_sense.tools.diagnostics.status

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.sense.mock.MockSensor
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.QualityColors
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings

class SensorStatusBadgeProvider(
    private val sensor: Sensor,
    private val icon: ImageVector
) : StatusBadgeProvider {

    private val formatter = FormatService.getInstance()
    private val isUnavailable = sensor is MockSensor

    override fun getBadge(): StatusBadge {
        if (isUnavailable) {
            return StatusBadge(
                getAppService<AppStrings>().get(Res.string.unavailable),
                QualityColors.of(Quality.Unknown),
                icon,
                isHealthy = false
            )
        }

        return StatusBadge(
            formatter.formatQuality(sensor.quality),
            QualityColors.of(sensor.quality),
            icon,
            isHealthy = sensor.quality == Quality.Good
        )
    }
}
