package com.kylecorry.andromeda.bitmaps

import com.kylecorry.sol.math.algebra.Matrix
import kotlin.math.roundToInt

/** Computes the gray-level co-occurrence matrix for the given neighbor offsets. */
fun PixelImage.glcm(
    steps: List<Pair<Int, Int>>,
    channel: ColorChannel? = null,
    excludeTransparent: Boolean = false,
    symmetric: Boolean = false,
    normalized: Boolean = true,
    levels: Int = 256,
): Matrix {
    require(levels in 1..256)
    val values = FloatArray(levels * levels)
    var count = 0L
    fun quantize(color: Int) = (color.channelValue(channel) * (levels - 1) / 255).roundToInt()
    for (y in 0 until height) for (x in 0 until width) {
        val pixel = this[x, y]
        if (excludeTransparent && (pixel ushr 24) == 0) continue
        val current = quantize(pixel)
        for ((dx, dy) in steps) {
            val nx = x.toLong() + dx
            val ny = y.toLong() + dy
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue
            val neighbor = this[nx.toInt(), ny.toInt()]
            if (excludeTransparent && (neighbor ushr 24) == 0) continue
            val adjacent = quantize(neighbor)
            values[current * levels + adjacent]++
            count++
            if (symmetric) {
                values[adjacent * levels + current]++
                count++
            }
        }
    }
    if (normalized && count > 0) for (index in values.indices) values[index] /= count.toFloat()
    return Matrix.create(levels, levels, values)
}
