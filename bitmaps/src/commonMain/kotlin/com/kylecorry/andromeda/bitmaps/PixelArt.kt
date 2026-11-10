package com.kylecorry.andromeda.bitmaps

import kotlin.math.abs

/** Doubles a pixel-art image using the xBR edge rules without introducing colors. */
public fun PixelImage.upscalePixelArt2x(): PixelImage {
    require(width.toLong() * height <= Int.MAX_VALUE / 4 && width <= Int.MAX_VALUE / 2 && height <= Int.MAX_VALUE / 2)
    val outputWidth = width * 2
    val output = IntArray(outputWidth * height * 2)
    fun get(x: Int, y: Int) = pixels[y.coerceIn(0, height - 1) * width + x.coerceIn(0, width - 1)]
    fun distance(first: Int, second: Int): Float {
        val r = abs(((first ushr 16) and 255) - ((second ushr 16) and 255))
        val g = abs(((first ushr 8) and 255) - ((second ushr 8) and 255))
        val b = abs((first and 255) - (second and 255))
        val a = abs((first ushr 24) - (second ushr 24))
        val luminance = abs(0.299 * r + 0.587 * g + 0.114 * b)
        val u = abs(-0.169 * r - 0.331 * g + 0.500 * b)
        val v = abs(0.500 * r - 0.419 * g - 0.081 * b)
        return (48 * luminance + 7 * u + 6 * v + 48 * a).toFloat()
    }
    for (y in 0 until height) for (x in 0 until width) {
        val a = get(x - 1, y - 1)
        val b = get(x, y - 1)
        val c = get(x + 1, y - 1)
        val d = get(x - 1, y)
        val e = get(x, y)
        val f = get(x + 1, y)
        val g = get(x - 1, y + 1)
        val h = get(x, y + 1)
        val i = get(x + 1, y + 1)
        val a1 = get(x - 1, y - 2)
        val b1 = get(x, y - 2)
        val c1 = get(x + 1, y - 2)
        val a0 = get(x - 2, y - 1)
        val c4 = get(x + 2, y - 1)
        val d0 = get(x - 2, y)
        val f4 = get(x + 2, y)
        val g0 = get(x - 2, y + 1)
        val i4 = get(x + 2, y + 1)
        val g5 = get(x - 1, y + 2)
        val h5 = get(x, y + 2)
        val i5 = get(x + 1, y + 2)
        var e0 = e
        var e1 = e
        var e2 = e
        var e3 = e
        val bottomRight = distance(e, c) + distance(e, g) + distance(i, f4) + distance(i, h5) + 4 * distance(h, f)
        val bottomRightAlternate = distance(h, d) + distance(h, i5) + distance(f, i4) + distance(f, b) + 4 * distance(e, i)
        if (bottomRight < bottomRightAlternate) {
            val color = if (distance(e, f) <= distance(e, h)) f else h
            e3 = color
            if (f == g) e2 = color
            if (h == c) e1 = color
        }
        val topRight = distance(e, i) + distance(e, a) + distance(c, f4) + distance(c, b1) + 4 * distance(b, f)
        val topRightAlternate = distance(b, d) + distance(b, c1) + distance(f, h) + distance(f, c4) + 4 * distance(e, c)
        if (topRight < topRightAlternate) {
            val color = if (distance(e, f) <= distance(e, b)) f else b
            e1 = color
            if (f == a) e0 = color
            if (b == i) e3 = color
        }
        val topLeft = distance(e, g) + distance(e, c) + distance(a, d0) + distance(a, b1) + 4 * distance(b, d)
        val topLeftAlternate = distance(b, f) + distance(b, a1) + distance(d, h) + distance(d, a0) + 4 * distance(e, a)
        if (topLeft < topLeftAlternate) {
            val color = if (distance(e, d) <= distance(e, b)) d else b
            e0 = color
            if (d == c) e1 = color
            if (b == g) e2 = color
        }
        val bottomLeft = distance(e, i) + distance(e, a) + distance(g, d0) + distance(g, h5) + 4 * distance(h, d)
        val bottomLeftAlternate = distance(h, f) + distance(h, g5) + distance(d, b) + distance(d, g0) + 4 * distance(e, g)
        if (bottomLeft < bottomLeftAlternate) {
            val color = if (distance(e, d) <= distance(e, h)) d else h
            e2 = color
            if (d == i) e3 = color
            if (h == a) e0 = color
        }
        val offset = y * 2 * outputWidth + x * 2
        output[offset] = e0
        output[offset + 1] = e1
        output[offset + outputWidth] = e2
        output[offset + outputWidth + 1] = e3
    }
    return PixelImage(outputWidth, height * 2, output)
}
