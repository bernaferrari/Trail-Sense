package com.kylecorry.trail_sense.shared.colors

import androidx.annotation.ColorInt

/** Plain ARGB colors for code that works with color ints and has no platform color class. */
object ColorInts {
    @ColorInt
    const val Black: Int = -0x1000000

    @ColorInt
    const val White: Int = -0x1

    @ColorInt
    const val Transparent: Int = 0

    @ColorInt
    const val Gray: Int = -0x777778

    @ColorInt
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    @ColorInt
    fun rgb(red: Int, green: Int, blue: Int): Int {
        return argb(255, red, green, blue)
    }

    /**
     * Mixes [color1] and [color2] channel by channel; [ratio] 0 is all [color1] and 1 is all [color2].
     * Matches AndroidX ColorUtils.blendARGB, truncating each channel.
     */
    @ColorInt
    fun blend(@ColorInt color1: Int, @ColorInt color2: Int, ratio: Float): Int {
        val inverse = 1 - ratio
        fun mix(shift: Int): Int {
            val a = (color1 ushr shift) and 0xFF
            val b = (color2 ushr shift) and 0xFF
            return (a * inverse + b * ratio).toInt()
        }
        return argb(mix(24), mix(16), mix(8), mix(0))
    }
}
