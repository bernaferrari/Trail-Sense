package com.kylecorry.andromeda.bitmaps

import kotlin.math.abs
import kotlin.math.sqrt

enum class ImageChannel { Red, Green, Blue, Alpha, RgbAverage }

data class ChannelStatistics(val minimum: Double, val maximum: Double, val mean: Double, val standardDeviation: Double)

data class ImagePoint(val x: Double, val y: Double)

private fun Int.channelValue(channel: ImageChannel): Double = when (channel) {
    ImageChannel.Red -> ((this ushr 16) and 255).toDouble()
    ImageChannel.Green -> ((this ushr 8) and 255).toDouble()
    ImageChannel.Blue -> (this and 255).toDouble()
    ImageChannel.Alpha -> ((this ushr 24) and 255).toDouble()
    ImageChannel.RgbAverage -> (((this ushr 16) and 255) + ((this ushr 8) and 255) + (this and 255)) / 3.0
}

/** Computes population statistics for one channel over the image. */
fun PixelImage.statistics(channel: ImageChannel = ImageChannel.RgbAverage): ChannelStatistics {
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
fun PixelImage.moment(channel: ImageChannel = ImageChannel.RgbAverage): ImagePoint {
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

/** Combines each ARGB channel independently, clamping results to the byte range. */
fun PixelImage.weightedAdd(other: PixelImage, weight: Float, otherWeight: Float, absolute: Boolean = false): PixelImage {
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
