package com.kylecorry.trail_sense.shared.scales

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.colors.ColorInts

class ContinuousColorScale(@ColorInt private val start: Int, @ColorInt private val end: Int) :
    IColorScale {
    override fun getColor(percent: Float): Int {
        return ColorInts.blend(start, end, percent)
    }
}
