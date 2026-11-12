package com.kylecorry.trail_sense.tools.weather.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.trail_sense.shared.icons.trail.*

class PressureCharacteristicImageMapper {

    fun getImageResource(characteristic: PressureCharacteristic): ImageVector {
        return when (characteristic) {
            PressureCharacteristic.Falling, PressureCharacteristic.FallingFast -> TrailIcons.ArrowDown
            PressureCharacteristic.Rising, PressureCharacteristic.RisingFast -> TrailIcons.ArrowUp
            else -> TrailIcons.SteadyArrow
        }
    }

}
