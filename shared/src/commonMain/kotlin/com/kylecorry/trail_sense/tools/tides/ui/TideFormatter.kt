package com.kylecorry.trail_sense.tools.tides.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.sol.science.oceanography.TideType
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.AppStrings

class TideFormatter {

    fun getTideTypeName(tideType: TideType?): String {
        return when (tideType) {
            TideType.High -> getAppService<AppStrings>().get(Res.string.high_tide)
            TideType.Low -> getAppService<AppStrings>().get(Res.string.low_tide)
            null -> getAppService<AppStrings>().get(Res.string.half_tide)
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
