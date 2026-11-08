package com.kylecorry.andromeda.bitmaps

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ceil

class FloatBitmap(val width: Int, val height: Int, val channels: Int) {
    val data = FloatArray(width * height * channels)

    fun get(x: Int, y: Int, channel: Int): Float = data[(y * width + x) * channels + channel]
    fun set(x: Int, y: Int, channel: Int, value: Float) { data[(y * width + x) * channels + channel] = value }
    fun getOrNull(x: Int, y: Int, channel: Int): Float? =
        if (x in 0 until width && y in 0 until height && channel in 0 until channels) get(x, y, channel) else null
    fun getX(index: Int): Int = index % width
    fun getY(index: Int): Int = index / width

    fun upscale(
        newWidth: Int, newHeight: Int, startX: Float = 0f, endX: Float = (width - 1).toFloat(),
        startY: Float = 0f, endY: Float = (height - 1).toFloat(), maxSearchRadius: Int = 10
    ): FloatBitmap {
        require(channels in 1..4 && newWidth > 0 && newHeight > 0)
        val result = FloatBitmap(newWidth, newHeight, channels)
        for (y in 0 until newHeight) for (x in 0 until newWidth) {
            val fx = if (newWidth > 1) startX + (endX - startX) * x / (newWidth - 1) else startX
            val fy = if (newHeight > 1) startY + (endY - startY) * y / (newHeight - 1) else startY
            val values = bicubic(fx, fy) ?: bilinear(fx, fy) ?: nearest(fx, fy, maxSearchRadius)
            for (channel in 0 until channels) result.set(x, y, channel, values?.get(channel) ?: Float.NaN)
        }
        return result
    }

    private fun pixel(x: Int, y: Int): FloatArray? {
        if (x !in 0 until width || y !in 0 until height) return null
        return FloatArray(channels) { get(x, y, it) }.takeIf { values -> values.none(Float::isNaN) }
    }
    private fun weight(value: Float): Float {
        val a = abs(value)
        return when {
            a <= 1 -> 1 - 2.5f * a * a + 1.5f * a * a * a
            a <= 2 -> 2 - 4 * a + 2.5f * a * a - 0.5f * a * a * a
            else -> 0f
        }
    }
    private fun bicubic(x: Float, y: Float): FloatArray? {
        val ix = floor(x).toInt()
        val iy = floor(y).toInt()
        val result = FloatArray(channels)
        for (channel in 0 until channels) {
            val rows = FloatArray(4)
            for (row in 0..3) for (column in 0..3) {
                val values = pixel(ix + column - 1, iy + row - 1) ?: return null
                rows[row] += values[channel] * weight(x - ix - (column - 1))
            }
            for (row in 0..3) result[channel] += rows[row] * weight(y - iy - (row - 1))
        }
        return result
    }
    private fun bilinear(x: Float, y: Float): FloatArray? {
        val ix = x.toInt()
        val iy = y.toInt()
        val points = listOf(pixel(ix, iy), pixel(ix + 1, iy), pixel(ix, iy + 1), pixel(ix + 1, iy + 1))
        if (points.any { it == null }) return null
        val dx = x - ix
        val dy = y - iy
        val weights = floatArrayOf((1 - dx) * (1 - dy), dx * (1 - dy), (1 - dx) * dy, dx * dy)
        return FloatArray(channels) { channel ->
            points[0]!![channel] * weights[0] + points[1]!![channel] * weights[1] +
                points[2]!![channel] * weights[2] + points[3]!![channel] * weights[3]
        }
    }
    private fun nearest(x: Float, y: Float, radius: Int): FloatArray? {
        val ix = if (x < 0) ceil(x - 0.5f).toInt() else floor(x + 0.5f).toInt()
        val iy = if (y < 0) ceil(y - 0.5f).toInt() else floor(y + 0.5f).toInt()
        for (r in 0..radius) {
            var best: FloatArray? = null
            var distance = Float.MAX_VALUE
            fun consider(cx: Int, cy: Int) {
                val candidate = pixel(cx, cy) ?: return
                val d = (cx - x) * (cx - x) + (cy - y) * (cy - y)
                if (d < distance) { distance = d; best = candidate }
            }
            if (r == 0) consider(ix, iy) else {
                for (cx in ix - r..ix + r) { consider(cx, iy - r); consider(cx, iy + r) }
                for (cy in iy - r + 1 until iy + r) { consider(ix - r, cy); consider(ix + r, cy) }
            }
            if (best != null) return best
        }
        return null
    }
}
