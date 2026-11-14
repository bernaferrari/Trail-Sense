package com.kylecorry.andromeda.bitmaps

import kotlin.math.exp
import kotlin.math.roundToInt

/** Gaussian blur with clamped edges and the same radius-to-sigma curve as the Android toolkit. */
public fun PixelImage.blur(radius: Int): PixelImage {
    require(radius in 0..25)
    if (radius == 0) return PixelImage(width, height, pixels.copyOf())
    require(pixels.size <= Int.MAX_VALUE / 4)
    val sigma = radius * 0.4 + 0.6
    val weights = DoubleArray(radius * 2 + 1) { index ->
        val distance = index - radius
        exp(-(distance * distance) / (2 * sigma * sigma))
    }
    val total = weights.sum()
    for (index in weights.indices) weights[index] /= total
    val intermediate = FloatArray(pixels.size * 4)
    for (y in 0 until height) for (x in 0 until width) {
        val destination = (y * width + x) * 4
        for (offset in -radius..radius) {
            val color = this[(x + offset).coerceIn(0, width - 1), y]
            val weight = weights[offset + radius].toFloat()
            for (channel in 0..3) intermediate[destination + channel] += ((color ushr (channel * 8)) and 255) * weight
        }
    }
    return PixelImage(width, height, IntArray(pixels.size) { index ->
        val x = index % width
        val y = index / width
        var color = 0
        for (channel in 0..3) {
            var value = 0.0
            for (offset in -radius..radius) {
                val source = ((y + offset).coerceIn(0, height - 1) * width + x) * 4 + channel
                value += intermediate[source] * weights[offset + radius]
            }
            color = color or (value.roundToInt().coerceIn(0, 255) shl (channel * 8))
        }
        color
    })
}
