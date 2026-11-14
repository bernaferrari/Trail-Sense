package com.kylecorry.andromeda.bitmaps

import kotlin.math.abs
import kotlin.math.sqrt

public data class ChannelStatistics(val minimum: Double, val maximum: Double, val mean: Double, val standardDeviation: Double)

public data class ImagePoint(val x: Double, val y: Double)

/** Bounds use inclusive left/top and exclusive right/bottom coordinates. */
public data class ImageBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

internal fun Int.channelValue(channel: ColorChannel?): Double = when (channel) {
    ColorChannel.Red -> ((this ushr 16) and 255).toDouble()
    ColorChannel.Green -> ((this ushr 8) and 255).toDouble()
    ColorChannel.Blue -> (this and 255).toDouble()
    ColorChannel.Alpha -> ((this ushr 24) and 255).toDouble()
    null -> (((this ushr 16) and 255) + ((this ushr 8) and 255) + (this and 255)) / 3.0
}

/** Computes population statistics for one channel over the image. */
public fun PixelImage.statistics(channel: ColorChannel? = null): ChannelStatistics {
    var minimum = Double.POSITIVE_INFINITY
    var maximum = Double.NEGATIVE_INFINITY
    var mean = 0.0
    var squaredDifferences = 0.0
    pixels.forEachIndexed { index, pixel ->
        val value = pixel.channelValue(channel)
        minimum = minOf(minimum, value)
        maximum = maxOf(maximum, value)
        val delta = value - mean
        mean += delta / (index + 1)
        squaredDifferences += delta * (value - mean)
    }
    return ChannelStatistics(minimum, maximum, mean, sqrt(squaredDifferences / pixels.size))
}

/** Returns the intensity-weighted center, or the origin for an entirely black channel. */
public fun PixelImage.moment(channel: ColorChannel? = null): ImagePoint {
    var total = 0.0
    var x = 0.0
    var y = 0.0
    pixels.forEachIndexed { index, pixel ->
        val value = pixel.channelValue(channel)
        total += value
        x += (index % width) * value
        y += (index / width) * value
    }
    return if (total == 0.0) ImagePoint(0.0, 0.0) else ImagePoint(x / total, y / total)
}

/** Finds four-connected regions at or above [threshold], largest bounding boxes first. */
public fun PixelImage.findBlobs(
    threshold: Double,
    channel: ColorChannel? = null,
    maxBlobs: Int = Int.MAX_VALUE,
): List<ImageBounds> {
    require(threshold.isFinite() && maxBlobs >= 0)
    if (maxBlobs == 0) return emptyList()
    val visited = BooleanArray(pixels.size)
    val queue = IntArray(pixels.size)
    val blobs = mutableListOf<ImageBounds>()
    for (start in pixels.indices) {
        if (visited[start]) continue
        visited[start] = true
        if (pixels[start].channelValue(channel) < threshold) continue
        var head = 0
        var tail = 0
        queue[tail++] = start
        var left = start % width
        var right = left
        var top = start / width
        var bottom = top
        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width
            left = minOf(left, x)
            right = maxOf(right, x)
            top = minOf(top, y)
            bottom = maxOf(bottom, y)
            fun visit(next: Int) {
                if (!visited[next]) {
                    visited[next] = true
                    if (pixels[next].channelValue(channel) >= threshold) queue[tail++] = next
                }
            }
            if (x > 0) visit(index - 1)
            if (x + 1 < width) visit(index + 1)
            if (y > 0) visit(index - width)
            if (y + 1 < height) visit(index + width)
        }
        blobs.add(ImageBounds(left, top, right + 1, bottom + 1))
    }
    return blobs.sortedByDescending { (it.right - it.left).toLong() * (it.bottom - it.top) }.take(maxBlobs)
}

/** Combines each ARGB channel independently, clamping results to the byte range. */
public fun PixelImage.weightedAdd(other: PixelImage, weight: Float, otherWeight: Float, absolute: Boolean = false): PixelImage {
    require(width == other.width && height == other.height)
    require(weight.isFinite() && otherWeight.isFinite())
    return PixelImage(width, height, IntArray(pixels.size) { index ->
        var result = 0
        for (shift in 0..24 step 8) {
            val value = ((pixels[index] ushr shift) and 255) * weight + ((other.pixels[index] ushr shift) and 255) * otherWeight
            val channel = (if (absolute) abs(value) else value).toInt().coerceIn(0, 255)
            result = result or (channel shl shift)
        }
        result
    })
}
