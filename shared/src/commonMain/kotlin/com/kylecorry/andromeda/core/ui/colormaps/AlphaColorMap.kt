package com.kylecorry.andromeda.core.ui.colormaps


class AlphaColorMap(private val baseColor: Int = PackedColor.BLACK, private val maxAlpha: Int = 255) :
    ColorMap {
    override fun getColor(percent: Float): Int {
        val value = percent.coerceIn(0f, 1f) * 255
        val scale = maxAlpha.coerceIn(0, 255) / 255f
        return PackedColor.withAlpha(baseColor, (value * scale).toInt())
    }
}