package com.kylecorry.trail_sense.shared.sensors

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.R

object CellSignalUtils {

    fun getCellQualityImage(quality: Quality?): ImageVector {
        return when (quality) {
            Quality.Poor -> TrailIcons.SignalCellular1
            Quality.Moderate -> TrailIcons.SignalCellular2
            Quality.Good -> TrailIcons.SignalCellular3
            else -> TrailIcons.SignalCellularOutline
        }
    }

}
