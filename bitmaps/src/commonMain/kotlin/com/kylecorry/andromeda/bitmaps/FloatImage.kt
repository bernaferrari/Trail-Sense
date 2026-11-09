package com.kylecorry.andromeda.bitmaps

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/** Resamples interleaved floating-point pixels, treating NaN as missing data. */
fun interpolateFloatImage(
    pixels: FloatArray,
    width: Int,
    height: Int,
    channels: Int,
    outputWidth: Int,
    outputHeight: Int,
    startX: Float = 0f,
    startY: Float = 0f,
    endX: Float = (width - 1).toFloat(),
    endY: Float = (height - 1).toFloat(),
    maxSearchRadius: Int = 10,
): FloatArray {
    require(width > 0 && height > 0 && channels > 0 && width.toLong() * height * channels == pixels.size.toLong())
    require(outputWidth > 0 && outputHeight > 0 && outputWidth.toLong() * outputHeight * channels <= Int.MAX_VALUE)
    require(startX.isFinite() && startY.isFinite() && endX.isFinite() && endY.isFinite() && maxSearchRadius >= 0)
    val output = FloatArray(outputWidth * outputHeight * channels) { Float.NaN }
    fun valid(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height &&
        (0 until channels).all { !pixels[(y * width + x) * channels + it].isNaN() }
    fun weight(t: Float): Float {
        val a = abs(t)
        return when {
            a <= 1f -> 1f - 2.5f * a * a + 1.5f * a * a * a
            a <= 2f -> 2f - 4f * a + 2.5f * a * a - 0.5f * a * a * a
            else -> 0f
        }
    }
    for (y in 0 until outputHeight) for (x in 0 until outputWidth) {
        val fx = if (outputWidth == 1) startX else startX + (endX - startX) * x / (outputWidth - 1)
        val fy = if (outputHeight == 1) startY else startY + (endY - startY) * y / (outputHeight - 1)
        val ix = floor(fx).toInt()
        val iy = floor(fy).toInt()
        val offset = (y * outputWidth + x) * channels
        val cubic = (-1..2).all { dy -> (-1..2).all { dx -> valid(ix + dx, iy + dy) } }
        if (cubic) {
            for (c in 0 until channels) {
                var value = 0f
                for (dy in -1..2) for (dx in -1..2) {
                    value += pixels[((iy + dy) * width + ix + dx) * channels + c] * weight(fx - ix - dx) * weight(fy - iy - dy)
                }
                output[offset + c] = value
            }
        } else if ((0..1).all { dy -> (0..1).all { dx -> valid(ix + dx, iy + dy) } }) {
            val dx = fx - ix
            val dy = fy - iy
            for (c in 0 until channels) {
                val top = pixels[(iy * width + ix) * channels + c] * (1 - dx) + pixels[(iy * width + ix + 1) * channels + c] * dx
                val bottom = pixels[((iy + 1) * width + ix) * channels + c] * (1 - dx) + pixels[((iy + 1) * width + ix + 1) * channels + c] * dx
                output[offset + c] = top * (1 - dy) + bottom * dy
            }
        } else {
            val centerX = fx.roundToInt()
            val centerY = fy.roundToInt()
            var bestDistance = Float.POSITIVE_INFINITY
            var bestX = 0
            var bestY = 0
            for (radius in 0..maxSearchRadius) {
                for (candidateY in centerY - radius..centerY + radius) for (candidateX in centerX - radius..centerX + radius) {
                    if (maxOf(abs(candidateX - centerX), abs(candidateY - centerY)) != radius || !valid(candidateX, candidateY)) continue
                    val distance = (candidateX - fx) * (candidateX - fx) + (candidateY - fy) * (candidateY - fy)
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestX = candidateX
                        bestY = candidateY
                    }
                }
                if (bestDistance < Float.POSITIVE_INFINITY) break
            }
            if (bestDistance < Float.POSITIVE_INFINITY) pixels.copyInto(output, offset, (bestY * width + bestX) * channels, (bestY * width + bestX + 1) * channels)
        }
    }
    return output
}
