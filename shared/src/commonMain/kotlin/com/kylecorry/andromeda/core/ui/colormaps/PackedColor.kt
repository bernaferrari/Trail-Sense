package com.kylecorry.andromeda.core.ui.colormaps

internal object PackedColor {
    const val BLACK: Int = -0x1000000
    fun rgb(red: Int, green: Int, blue: Int): Int = BLACK or (red shl 16) or (green shl 8) or blue
    fun withAlpha(color: Int, alpha: Int): Int = (alpha shl 24) or (color and 0xFFFFFF)
    fun interpolate(first: Int, second: Int, factor: Float): Int {
        fun channel(shift: Int): Int {
            val a = (first ushr shift) and 255
            val b = (second ushr shift) and 255
            return (a + (b - a) * factor).toInt()
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
