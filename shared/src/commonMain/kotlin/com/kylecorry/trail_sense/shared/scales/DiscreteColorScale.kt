package com.kylecorry.trail_sense.shared.scales

import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.shared.colors.ColorInts

class DiscreteColorScale(@ColorInt private val colors: List<Int>) :
    IColorScale {
    override fun getColor(percent: Float): Int {
        if (colors.isEmpty()) {
            return ColorInts.Black
        }

        val percentPerColor = 1 / colors.size.toFloat()

        for (i in colors.indices) {
            if (percent <= percentPerColor * (i + 1)) {
                return colors[i]
            }
        }

        return colors.last()
    }
}
