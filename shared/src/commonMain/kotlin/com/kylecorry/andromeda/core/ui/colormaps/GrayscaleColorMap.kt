package com.kylecorry.andromeda.core.ui.colormaps


public class GrayscaleColorMap : ColorMap {
    override fun getColor(percent: Float): Int {
        val value = (percent * 255).toInt()
        return PackedColor.rgb(value, value, value)
    }
}