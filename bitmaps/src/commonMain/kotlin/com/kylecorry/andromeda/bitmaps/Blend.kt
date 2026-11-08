package com.kylecorry.andromeda.bitmaps

import kotlin.math.roundToInt

/** Blends [source] over this destination, using straight-alpha ARGB pixels. */
fun PixelImage.blend(source: PixelImage, mode: BlendingMode = BlendingMode.SRC_OVER): PixelImage {
    require(width == source.width && height == source.height)
    return PixelImage(width, height, IntArray(pixels.size) { index -> blendPixel(source.pixels[index], pixels[index], mode) })
}

private fun blendPixel(source: Int, destination: Int, mode: BlendingMode): Int {
    when (mode) {
        BlendingMode.CLEAR -> return 0
        BlendingMode.SRC -> return source
        BlendingMode.DST -> return destination
        BlendingMode.XOR -> return source xor destination
        else -> Unit
    }
    val sourceAlpha = (source ushr 24) / 255f
    val destinationAlpha = (destination ushr 24) / 255f
    val sourceFactor: Float
    val destinationFactor: Float
    when (mode) {
        BlendingMode.SRC_OVER -> { sourceFactor = 1f; destinationFactor = 1f - sourceAlpha }
        BlendingMode.DST_OVER -> { sourceFactor = 1f - destinationAlpha; destinationFactor = 1f }
        BlendingMode.SRC_IN -> { sourceFactor = destinationAlpha; destinationFactor = 0f }
        BlendingMode.DST_IN -> { sourceFactor = 0f; destinationFactor = sourceAlpha }
        BlendingMode.SRC_OUT -> { sourceFactor = 1f - destinationAlpha; destinationFactor = 0f }
        BlendingMode.DST_OUT -> { sourceFactor = 0f; destinationFactor = 1f - sourceAlpha }
        BlendingMode.SRC_ATOP -> { sourceFactor = destinationAlpha; destinationFactor = 1f - sourceAlpha }
        BlendingMode.DST_ATOP -> { sourceFactor = 1f - destinationAlpha; destinationFactor = sourceAlpha }
        else -> { sourceFactor = 0f; destinationFactor = 0f }
    }
    fun combine(sourceValue: Float, destinationValue: Float): Float = when (mode) {
        BlendingMode.MULTIPLY -> sourceValue * destinationValue
        BlendingMode.ADD -> (sourceValue + destinationValue).coerceAtMost(1f)
        BlendingMode.SUBTRACT -> (destinationValue - sourceValue).coerceAtLeast(0f)
        else -> sourceValue * sourceFactor + destinationValue * destinationFactor
    }
    val alpha = combine(sourceAlpha, destinationAlpha).coerceIn(0f, 1f)
    if (alpha <= 0f) return 0
    var result = (alpha * 255).roundToInt().coerceIn(0, 255) shl 24
    for (shift in 0..16 step 8) {
        val sourceValue = ((source ushr shift) and 255) / 255f * sourceAlpha
        val destinationValue = ((destination ushr shift) and 255) / 255f * destinationAlpha
        val channel = (combine(sourceValue, destinationValue) / alpha * 255).roundToInt().coerceIn(0, 255)
        result = result or (channel shl shift)
    }
    return result
}
