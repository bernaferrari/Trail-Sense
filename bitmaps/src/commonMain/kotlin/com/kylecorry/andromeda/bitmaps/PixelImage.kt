package com.kylecorry.andromeda.bitmaps

enum class ImageInterpolation { Nearest, Bilinear, Bicubic }

/** An ARGB image that can be processed without a platform bitmap. */
class PixelImage(val width: Int, val height: Int, val pixels: IntArray) {
    init {
        require(width > 0 && height > 0 && width.toLong() * height == pixels.size.toLong())
    }

    operator fun get(x: Int, y: Int): Int {
        require(x in 0 until width && y in 0 until height)
        return pixels[y * width + x]
    }

    fun map(transform: (Int) -> Int): PixelImage = PixelImage(width, height, IntArray(pixels.size) { transform(pixels[it]) })

    fun crop(left: Int, top: Int, width: Int, height: Int): PixelImage {
        require(left >= 0 && top >= 0 && width > 0 && height > 0 && left.toLong() + width <= this.width && top.toLong() + height <= this.height)
        return PixelImage(width, height, IntArray(width * height) { index -> this[left + index % width, top + index / width] })
    }

    fun flip(horizontal: Boolean = false, vertical: Boolean = false): PixelImage =
        PixelImage(width, height, IntArray(pixels.size) { index ->
            val x = index % width
            val y = index / width
            this[if (horizontal) width - 1 - x else x, if (vertical) height - 1 - y else y]
        })

    fun resize(width: Int, height: Int, interpolation: ImageInterpolation = ImageInterpolation.Nearest): PixelImage {
        require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE)
        return PixelImage(width, height, IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (interpolation == ImageInterpolation.Nearest) {
                this[((x.toLong() * this.width) / width).toInt(), ((y.toLong() * this.height) / height).toInt()]
            } else {
                val sourceX = ((x + 0.5) * this.width / width - 0.5).coerceIn(0.0, this.width - 1.0)
                val sourceY = ((y + 0.5) * this.height / height - 0.5).coerceIn(0.0, this.height - 1.0)
                when (interpolation) {
                    ImageInterpolation.Bilinear -> interpolate(sourceX.toFloat(), sourceY.toFloat())!!
                    ImageInterpolation.Bicubic -> interpolateBicubic(sourceX, sourceY)
                    ImageInterpolation.Nearest -> error("Unreachable")
                }
            }
        })
    }

    fun pad(left: Int, top: Int, right: Int, bottom: Int, color: Int = 0): PixelImage {
        require(left >= 0 && top >= 0 && right >= 0 && bottom >= 0)
        val newWidth = width.toLong() + left + right
        val newHeight = height.toLong() + top + bottom
        require(newWidth <= Int.MAX_VALUE && newHeight <= Int.MAX_VALUE && newWidth * newHeight <= Int.MAX_VALUE)
        val result = IntArray((newWidth * newHeight).toInt()) { color }
        repeat(height) { y -> pixels.copyInto(result, (y + top) * newWidth.toInt() + left, y * width, (y + 1) * width) }
        return PixelImage(newWidth.toInt(), newHeight.toInt(), result)
    }

    /** Replaces colors within an RGBA distance, optionally fading the replacement toward the tolerance boundary. */
    fun replaceColor(from: Int, to: Int, tolerance: Float = 0f, interpolate: Boolean = false): PixelImage {
        require(tolerance.isFinite() && tolerance >= 0f)
        return map { color ->
            var distanceSquared = 0
            for (shift in 0..24 step 8) {
                val difference = ((color ushr shift) and 255) - ((from ushr shift) and 255)
                distanceSquared += difference * difference
            }
            val distance = kotlin.math.sqrt(distanceSquared.toFloat())
            when {
                distance > tolerance -> color
                !interpolate || tolerance == 0f -> to
                else -> {
                    val ratio = 1f - distance / tolerance
                    var replacement = 0
                    for (shift in 0..24 step 8) {
                        val original = (color ushr shift) and 255
                        val target = (to ushr shift) and 255
                        replacement = replacement or ((original + ratio * (target - original)).toInt().coerceIn(0, 255) shl shift)
                    }
                    replacement
                }
            }
        }
    }
}

private fun PixelImage.interpolateBicubic(x: Double, y: Double): Int {
    val left = kotlin.math.floor(x).toInt()
    val top = kotlin.math.floor(y).toInt()
    val dx = x - left
    val dy = y - top
    fun cubic(a: Double, b: Double, c: Double, d: Double, fraction: Double): Double =
        b + 0.5 * fraction * (c - a + fraction * (2 * a - 5 * b + 4 * c - d + fraction * (3 * (b - c) + d - a)))
    var color = 0
    for (shift in 0..24 step 8) {
        fun row(offset: Int): Double {
            val sampleY = (top + offset).coerceIn(0, height - 1)
            fun sample(column: Int): Double = ((this[(left + column).coerceIn(0, width - 1), sampleY] ushr shift) and 255).toDouble()
            return cubic(sample(-1), sample(0), sample(1), sample(2), dx)
        }
        val channel = kotlin.math.round(cubic(row(-1), row(0), row(1), row(2), dy)).toInt().coerceIn(0, 255)
        color = color or (channel shl shift)
    }
    return color
}

/** Converts RGB to grayscale while preserving alpha. */
fun PixelImage.grayscale(): PixelImage = map { color ->
    val red = (color ushr 16) and 0xff
    val green = (color ushr 8) and 0xff
    val blue = color and 0xff
    val gray = (0.299f * red + 0.587f * green + 0.114f * blue).toInt().coerceIn(0, 255)
    (color and -0x1000000) or (gray shl 16) or (gray shl 8) or gray
}

/** Counts pixels in each of the 256 grayscale luminance buckets. */
fun PixelImage.luminanceHistogram(): IntArray {
    val histogram = IntArray(256)
    for (color in pixels) {
        val red = (color ushr 16) and 0xff
        val green = (color ushr 8) and 0xff
        val blue = color and 0xff
        val luminance = (0.299f * red + 0.587f * green + 0.114f * blue).toInt().coerceIn(0, 255)
        histogram[luminance]++
    }
    return histogram
}

/** Replaces each pixel with black or white based on normalized luminance. */
fun PixelImage.threshold(luminance: Float): PixelImage {
    require(luminance in 0f..1f)
    return grayscale().map { color ->
        val value = if ((color and 0xff) / 255f >= luminance) 0x00ffffff else 0
        (color and -0x1000000) or value
    }
}

/** Thresholds one channel, or average RGB when [channel] is null, on the 0–255 scale. */
fun PixelImage.threshold(value: Float, binary: Boolean, channel: ColorChannel? = null): PixelImage {
    require(value.isFinite())
    return map { color ->
        val above = color.channelValue(channel) > value
        if (above && !binary) color else {
            val replacement = if (above) 255 else 0
            if (channel == null) {
                (color and -0x1000000) or (replacement * 0x010101)
            } else {
                val shift = when (channel) {
                    ColorChannel.Red -> 16
                    ColorChannel.Green -> 8
                    ColorChannel.Blue -> 0
                    ColorChannel.Alpha -> 24
                }
                (color and (255 shl shift).inv()) or (replacement shl shift)
            }
        }
    }
}

/** Applies an odd square convolution kernel to ARGB channels with clamped edges. */
fun PixelImage.convolve(kernel: FloatArray): PixelImage {
    val side = kotlin.math.sqrt(kernel.size.toDouble()).toInt()
    require(side > 0 && side % 2 == 1 && side * side == kernel.size)
    val radius = side / 2
    return PixelImage(width, height, IntArray(pixels.size) { index ->
        val x = index % width
        val y = index / width
        val channels = FloatArray(4)
        for (ky in 0 until side) for (kx in 0 until side) {
            val pixel = this[(x + kx - radius).coerceIn(0, width - 1), (y + ky - radius).coerceIn(0, height - 1)]
            val weight = kernel[ky * side + kx]
            channels[0] += ((pixel ushr 24) and 0xff) * weight
            channels[1] += ((pixel ushr 16) and 0xff) * weight
            channels[2] += ((pixel ushr 8) and 0xff) * weight
            channels[3] += (pixel and 0xff) * weight
        }
        (channels[0].toInt().coerceIn(0, 255) shl 24) or
            (channels[1].toInt().coerceIn(0, 255) shl 16) or
            (channels[2].toInt().coerceIn(0, 255) shl 8) or channels[3].toInt().coerceIn(0, 255)
    })
}

/** Returns the bilinearly interpolated ARGB value, or null outside the image. */
fun PixelImage.interpolate(x: Float, y: Float): Int? {
    if (!x.isFinite() || !y.isFinite() || x < 0f || y < 0f || x > width - 1 || y > height - 1) return null
    val left = x.toInt()
    val top = y.toInt()
    val right = (left + 1).coerceAtMost(width - 1)
    val bottom = (top + 1).coerceAtMost(height - 1)
    val dx = x - left
    val dy = y - top
    val values = intArrayOf(this[left, top], this[right, top], this[left, bottom], this[right, bottom])
    val weights = floatArrayOf((1f - dx) * (1f - dy), dx * (1f - dy), (1f - dx) * dy, dx * dy)
    var result = 0
    for (shift in 0..24 step 8) {
        var channel = 0f
        for (i in 0..3) channel += ((values[i] ushr shift) and 0xff) * weights[i]
        result = result or (channel.toInt().coerceIn(0, 255) shl shift)
    }
    return result
}

/** Reduces each RGB channel to the given number of evenly spaced values. */
fun PixelImage.quantize(levels: Int): PixelImage {
    require(levels in 2..256)
    fun channel(value: Int): Int {
        val bucket = (value * (levels - 1) + 127) / 255
        return (bucket * 255 + (levels - 1) / 2) / (levels - 1)
    }
    return map { color ->
        val red = channel((color ushr 16) and 0xff)
        val green = channel((color ushr 8) and 0xff)
        val blue = channel(color and 0xff)
        (color and -0x1000000) or (red shl 16) or (green shl 8) or blue
    }
}
