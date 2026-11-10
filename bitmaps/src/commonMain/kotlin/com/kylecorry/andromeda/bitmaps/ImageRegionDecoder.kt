package com.kylecorry.andromeda.bitmaps

import kotlin.math.roundToInt

/** Decodes rectangular regions of an encoded image. Close the decoder when finished. */
public expect class ImageRegionDecoder : AutoCloseable {
    public constructor(bytes: ByteArray)
    public constructor(path: String)
    public val width: Int
    public val height: Int
    /** Region coordinates use the source image's top-left origin and must be fully inside it. */
    public fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage
    override fun close()
}

internal fun requireImageRegion(left: Int, top: Int, width: Int, height: Int, imageWidth: Int, imageHeight: Int) {
    require(left >= 0 && top >= 0 && width > 0 && height > 0 &&
        left.toLong() + width <= imageWidth && top.toLong() + height <= imageHeight) { "Region is outside the image" }
}

/**
 * Decodes a region that may extend beyond the image, filling uncovered pixels with [background].
 * When [wrapHorizontally] is true the source repeats horizontally, including across multiple wraps.
 */
public fun ImageRegionDecoder.decodeWrappedRegion(
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    wrapHorizontally: Boolean = false,
    background: Int = 0,
): PixelImage {
    require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE)
    val output = IntArray(width * height) { background }
    val startY = maxOf(0L, top.toLong())
    val endY = minOf(this.height.toLong(), top.toLong() + height)
    if (startY >= endY) return PixelImage(width, height, output)
    val rows = (endY - startY).toInt()
    val destinationY = (startY - top.toLong()).toInt()

    fun copyPart(sourceX: Int, destinationX: Int, columns: Int) {
        val part = decode(sourceX, startY.toInt(), columns, rows)
        repeat(rows) { row ->
            part.pixels.copyInto(output, (destinationY + row) * width + destinationX, row * columns, (row + 1) * columns)
        }
    }

    if (wrapHorizontally) {
        var destinationX = 0
        while (destinationX < width) {
            val position = (left.toLong() + destinationX) % this.width
            val sourceX = ((position + this.width) % this.width).toInt()
            val columns = minOf(width - destinationX, this.width - sourceX)
            copyPart(sourceX, destinationX, columns)
            destinationX += columns
        }
    } else {
        val startX = maxOf(0L, left.toLong())
        val endX = minOf(this.width.toLong(), left.toLong() + width)
        if (startX < endX) copyPart(startX.toInt(), (startX - left.toLong()).toInt(), (endX - startX).toInt())
    }
    return PixelImage(width, height, output)
}

/** Samples a pixel without decoding the whole image; returns null for coordinates outside it. */
public fun ImageRegionDecoder.samplePixel(x: Float, y: Float, interpolate: Boolean = true): Int? {
    if (!x.isFinite() || !y.isFinite() || x < 0f || y < 0f || x > width - 1 || y > height - 1) return null
    if (!interpolate) {
        val pixelX = x.roundToInt().coerceIn(0, width - 1)
        val pixelY = y.roundToInt().coerceIn(0, height - 1)
        return decode(pixelX, pixelY, 1, 1).pixels[0]
    }
    val left = x.toInt()
    val top = y.toInt()
    val columns = minOf(2, width - left)
    val rows = minOf(2, height - top)
    return decode(left, top, columns, rows).interpolate(x - left, y - top)
}
