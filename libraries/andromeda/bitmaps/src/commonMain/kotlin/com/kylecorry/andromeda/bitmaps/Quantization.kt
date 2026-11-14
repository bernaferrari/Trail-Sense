package com.kylecorry.andromeda.bitmaps

import kotlin.math.roundToInt

/** Counts the 256 values of one color channel. */
public fun PixelImage.histogram(channel: ColorChannel): IntArray {
    val shift = when (channel) {
        ColorChannel.Red -> 16
        ColorChannel.Green -> 8
        ColorChannel.Blue -> 0
        ColorChannel.Alpha -> 24
    }
    return IntArray(256).also { counts -> pixels.forEach { counts[(it ushr shift) and 255]++ } }
}

/** Reduces each channel independently, optionally distributing quantization error to nearby pixels. */
public fun PixelImage.quantize(
    redLevels: Int,
    greenLevels: Int,
    blueLevels: Int,
    alphaLevels: Int = 256,
    dither: Boolean = false,
): PixelImage {
    val levels = intArrayOf(redLevels, greenLevels, blueLevels, alphaLevels)
    require(levels.all { it in 2..256 })
    val shifts = intArrayOf(16, 8, 0, 24)
    fun quantizeChannel(value: Float, count: Int): Int {
        val bucket = (value.coerceIn(0f, 255f) * (count - 1) / 255f).roundToInt()
        return (bucket * 255f / (count - 1)).roundToInt()
    }
    if (!dither) return map { pixel ->
        var result = 0
        for (channel in 0..3) result = result or (quantizeChannel(((pixel ushr shifts[channel]) and 255).toFloat(), levels[channel]) shl shifts[channel])
        result
    }
    require(width <= Int.MAX_VALUE / 4 - 2)
    var current = FloatArray((width + 2) * 4)
    var next = FloatArray(current.size)
    val output = IntArray(pixels.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = pixels[y * width + x]
            var result = 0
            for (channel in 0..3) {
                val offset = (x + 1) * 4 + channel
                val value = (((pixel ushr shifts[channel]) and 255) + current[offset]).coerceIn(0f, 255f)
                val quantized = quantizeChannel(value, levels[channel])
                result = result or (quantized shl shifts[channel])
                val error = value - quantized
                current[offset + 4] += error * (7f / 16f)
                next[offset - 4] += error * (3f / 16f)
                next[offset] += error * (5f / 16f)
                next[offset + 4] += error * (1f / 16f)
            }
            output[y * width + x] = result
        }
        val previous = current
        current = next
        next = previous
        next.fill(0f)
    }
    return PixelImage(width, height, output)
}
