package com.kylecorry.andromeda.core.ui

import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.sol.math.interpolation.Interpolation
import kotlin.math.absoluteValue

object Colors {
    private fun red(color: Int) = (color ushr 16) and 0xff
    private fun green(color: Int) = (color ushr 8) and 0xff
    private fun blue(color: Int) = color and 0xff
    private fun alpha(color: Int) = (color ushr 24) and 0xff
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    private fun rgb(r: Int, g: Int, b: Int) = argb(255, r, g, b)


    fun mostContrastingColor(
        foreground1: Int,
        foreground2: Int,
        background: Int
    ): Int {
        // From https://newbedev.com/how-to-programmatically-calculate-the-contrast-ratio-between-two-colors
        val f1 = (299 * red(foreground1) + 587 * green(foreground1) + 114 * blue(
            foreground1
        )) / 1000f

        val f2 = (299 * red(foreground2) + 587 * green(foreground2) + 114 * blue(
            foreground2
        )) / 1000f

        val b = (299 * red(background) + 587 * green(background) + 114 * blue(
            background
        )) / 1000f

        val r1 = f1 - b
        val r2 = f2 - b

        return if (r1.absoluteValue > r2.absoluteValue) foreground1 else foreground2
    }

    fun Int.withAlpha(alpha: Int): Int {
        return argb(
            alpha,
            red(this),
            green(this),
            blue(this)
        )
    }

    fun toHex(color: Int): String {
        return "#" + (0xFFFFFF and color).toString(16).padStart(6, '0').uppercase()
    }

    fun interpolate(color1: Int, color2: Int, factor: Float): Int {
        val a1 = alpha(color1)
        val r1 = red(color1)
        val g1 = green(color1)
        val b1 = blue(color1)
        val a2 = alpha(color2)
        val r2 = red(color2)
        val g2 = green(color2)
        val b2 = blue(color2)
        val a = (a1 + ((a2 - a1) * factor)).toInt()
        val r = (r1 + ((r2 - r1) * factor)).toInt()
        val g = (g1 + ((g2 - g1) * factor)).toInt()
        val b = (b1 + ((b2 - b1) * factor)).toInt()
        return argb(a, r, g, b)
    }

    fun fromColorTemperature(temp: Float): Int {
        // http://www.vendian.org/mncharity/dir3/blackbody/UnstableURLs/bbr_color.html
        val map = RgbInterpolationColorMap(
            arrayOf(
                rgb(255, 51, 0), // 1000K
                rgb(255, 137, 18), // 2000K
                rgb(255, 185, 105), // 3000K
                rgb(255, 209, 163), // 4000K
                rgb(255, 231, 204), // 5000K
                rgb(255, 244, 237), // 6000K
                rgb(245, 243, 255), // 7000K
                rgb(229, 233, 255), // 8000K
                rgb(217, 225, 255), // 9000K
                rgb(207, 218, 255), // 10000K
            )
        )

        val percent = Interpolation.map(temp, 1000f, 10000f, 0f, 1f, true)

        return map.getColor(percent)
    }
}
