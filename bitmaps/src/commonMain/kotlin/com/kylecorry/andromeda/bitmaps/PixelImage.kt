package com.kylecorry.andromeda.bitmaps

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

    fun resize(width: Int, height: Int): PixelImage {
        require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE)
        return PixelImage(width, height, IntArray(width * height) { index ->
            val x = (index % width).toLong() * this.width / width
            val y = (index / width).toLong() * this.height / height
            this[x.toInt(), y.toInt()]
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

    fun replaceColor(from: Int, to: Int): PixelImage = map { if (it == from) to else it }
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
