package com.kylecorry.andromeda.bitmaps

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

public class FloatBitmap(public val width: Int, public val height: Int, public val channels: Int) {
    init {
        require(width > 0 && height > 0 && channels > 0)
        require(width.toLong() * height <= Int.MAX_VALUE / channels)
    }

    public val data: FloatArray = FloatArray(width * height * channels)

    public fun get(x: Int, y: Int, channel: Int): Float {
        require(x in 0 until width && y in 0 until height && channel in 0 until channels)
        return data[(y * width + x) * channels + channel]
    }
    public fun set(x: Int, y: Int, channel: Int, value: Float) {
        require(x in 0 until width && y in 0 until height && channel in 0 until channels)
        data[(y * width + x) * channels + channel] = value
    }
    public fun getOrNull(x: Int, y: Int, channel: Int): Float? =
        if (x in 0 until width && y in 0 until height && channel in 0 until channels) data[(y * width + x) * channels + channel] else null
    public fun getX(index: Int): Int = index % width
    public fun getY(index: Int): Int = index / width

    public fun upscale(
        newWidth: Int, newHeight: Int, startX: Float = 0f, endX: Float = (width - 1).toFloat(),
        startY: Float = 0f, endY: Float = (height - 1).toFloat(), maxSearchRadius: Int = 10
    ): FloatBitmap {
        require(channels in 1..4 && newWidth > 0 && newHeight > 0 && maxSearchRadius >= 0)
        require(startX.isFinite() && endX.isFinite() && startY.isFinite() && endY.isFinite())
        val result = FloatBitmap(newWidth, newHeight, channels)
        for (y in 0 until newHeight) for (x in 0 until newWidth) {
            val fx = if (newWidth > 1) startX + (endX - startX) * x / (newWidth - 1) else startX
            val fy = if (newHeight > 1) startY + (endY - startY) * y / (newHeight - 1) else startY
            val outputOffset = (y * newWidth + x) * channels
            if (!bicubic(fx, fy, result.data, outputOffset) && !bilinear(fx, fy, result.data, outputOffset) &&
                !nearest(fx, fy, maxSearchRadius, result.data, outputOffset)) {
                for (channel in 0 until channels) result.data[outputOffset + channel] = Float.NaN
            }
        }
        return result
    }

    private fun validPixel(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        val offset = (y * width + x) * channels
        for (channel in 0 until channels) if (data[offset + channel].isNaN()) return false
        return true
    }
    private fun weight(value: Float): Float {
        val a = abs(value)
        return when {
            a <= 1 -> 1 - 2.5f * a * a + 1.5f * a * a * a
            a <= 2 -> 2 - 4 * a + 2.5f * a * a - 0.5f * a * a * a
            else -> 0f
        }
    }
    private fun bicubic(x: Float, y: Float, output: FloatArray, outputOffset: Int): Boolean {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        for (row in -1..2) for (column in -1..2) if (!validPixel(ix + column, iy + row)) return false
        for (channel in 0 until channels) {
            var total = 0f
            for (row in -1..2) {
                var rowValue = 0f
                for (column in -1..2) {
                    val index = ((iy + row) * width + ix + column) * channels + channel
                    rowValue += data[index] * weight(x - ix - column)
                }
                total += rowValue * weight(y - iy - row)
            }
            output[outputOffset + channel] = total
        }
        return true
    }
    private fun bilinear(x: Float, y: Float, output: FloatArray, outputOffset: Int): Boolean {
        val ix = x.toInt()
        val iy = y.toInt()
        if (!validPixel(ix, iy) || !validPixel(ix + 1, iy) || !validPixel(ix, iy + 1) || !validPixel(ix + 1, iy + 1)) return false
        val dx = x - ix
        val dy = y - iy
        val first = (iy * width + ix) * channels
        val second = first + width * channels
        for (channel in 0 until channels) {
            output[outputOffset + channel] = data[first + channel] * (1 - dx) * (1 - dy) +
                data[first + channels + channel] * dx * (1 - dy) + data[second + channel] * (1 - dx) * dy +
                data[second + channels + channel] * dx * dy
        }
        return true
    }
    private fun nearest(x: Float, y: Float, radius: Int, output: FloatArray, outputOffset: Int): Boolean {
        val ix = if (x < 0) ceil(x - 0.5f).toInt() else floor(x + 0.5f).toInt()
        val iy = if (y < 0) ceil(y - 0.5f).toInt() else floor(y + 0.5f).toInt()
        for (r in 0..radius) {
            var bestOffset = -1
            var distance = Float.MAX_VALUE
            fun consider(cx: Int, cy: Int) {
                if (!validPixel(cx, cy)) return
                val d = (cx - x) * (cx - x) + (cy - y) * (cy - y)
                if (d < distance) { distance = d; bestOffset = (cy * width + cx) * channels }
            }
            if (r == 0) consider(ix, iy) else {
                for (cx in ix - r..ix + r) { consider(cx, iy - r); consider(cx, iy + r) }
                for (cy in iy - r + 1 until iy + r) { consider(ix - r, cy); consider(ix + r, cy) }
            }
            if (bestOffset >= 0) {
                data.copyInto(output, outputOffset, bestOffset, bestOffset + channels)
                return true
            }
        }
        return false
    }
}
