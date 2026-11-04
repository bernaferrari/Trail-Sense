package com.kylecorry.trail_sense.tools.tides.ui

import android.content.Context
import com.kylecorry.sol.science.oceanography.TideType
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*

class TideFormatter(private val context: Context) {

    fun getTideTypeName(tideType: TideType?): String {
        return when (tideType) {
            TideType.High -> context.getString(R.string.high_tide)
            TideType.Low -> context.getString(R.string.low_tide)
            null -> context.getString(R.string.half_tide)
        }
    }

    fun getTideTypeImage(tideType: TideType?): ImageVector {
        return when (tideType) {
            TideType.High -> TrailIcons.TideHigh
            TideType.Low -> TrailIcons.TideLow
            null -> TrailIcons.TideHalf
        }
    }

    fun getTideTypeIcon(tideType: TideType?): ImageVector {
        return when (tideType) {
            TideType.High -> TrailIcons.TideHigh
            TideType.Low -> TrailIcons.TideLow
            null -> TrailIcons.TideHalf
        }
    }
}
