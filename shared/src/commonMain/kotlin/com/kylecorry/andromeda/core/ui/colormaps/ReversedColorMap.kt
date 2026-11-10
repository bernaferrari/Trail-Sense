package com.kylecorry.andromeda.core.ui.colormaps

public class ReversedColorMap(private val map: ColorMap) : ColorMap {
    override fun getColor(percent: Float): Int {
        return map.getColor(1 - percent.coerceIn(0f, 1f))
    }
}