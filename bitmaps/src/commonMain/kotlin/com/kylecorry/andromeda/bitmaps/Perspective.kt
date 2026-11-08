package com.kylecorry.andromeda.bitmaps

import com.kylecorry.andromeda.core.units.PixelBounds
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.math.abs
import kotlin.math.hypot

/** Rectifies a four-corner image region. Coordinates describe the outer pixel edges. */
fun PixelImage.correctPerspective(
    bounds: PixelBounds,
    outputWidth: Int = averageDistance(bounds.topLeft, bounds.topRight, bounds.bottomLeft, bounds.bottomRight),
    outputHeight: Int = averageDistance(bounds.topLeft, bounds.bottomLeft, bounds.topRight, bounds.bottomRight),
    background: Int = 0,
): PixelImage {
    require(outputWidth > 0 && outputHeight > 0 && outputWidth.toLong() * outputHeight <= Int.MAX_VALUE)
    val points = listOf(bounds.topLeft, bounds.topRight, bounds.bottomRight, bounds.bottomLeft)
    require(points.all { it.x.isFinite() && it.y.isFinite() })
    val crosses = points.indices.map { index ->
        val a = points[index]
        val b = points[(index + 1) % 4]
        val c = points[(index + 2) % 4]
        (b.x - a.x).toDouble() * (c.y - b.y) - (b.y - a.y).toDouble() * (c.x - b.x)
    }
    require(crosses.all { it > 0 } || crosses.all { it < 0 }) { "Perspective bounds must form a convex quadrilateral" }
    val x = points.map { it.x.toDouble() }
    val y = points.map { it.y.toDouble() }
    val dx1 = x[1] - x[2]
    val dx2 = x[3] - x[2]
    val dx3 = x[0] - x[1] + x[2] - x[3]
    val dy1 = y[1] - y[2]
    val dy2 = y[3] - y[2]
    val dy3 = y[0] - y[1] + y[2] - y[3]
    val divisor = dx1 * dy2 - dx2 * dy1
    require(abs(divisor) > 1e-12) { "Perspective bounds are degenerate" }
    val g = (dx3 * dy2 - dx2 * dy3) / divisor
    val h = (dx1 * dy3 - dx3 * dy1) / divisor
    val a = x[1] - x[0] + g * x[1]
    val b = x[3] - x[0] + h * x[3]
    val d = y[1] - y[0] + g * y[1]
    val e = y[3] - y[0] + h * y[3]
    return PixelImage(outputWidth, outputHeight, IntArray(outputWidth * outputHeight) { index ->
        val u = (index % outputWidth + 0.5) / outputWidth
        val v = (index / outputWidth + 0.5) / outputHeight
        val denominator = g * u + h * v + 1
        val sourceX = (a * u + b * v + x[0]) / denominator
        val sourceY = (d * u + e * v + y[0]) / denominator
        if (sourceX < 0 || sourceY < 0 || sourceX > width || sourceY > height || !sourceX.isFinite() || !sourceY.isFinite()) {
            background
        } else {
            interpolate((sourceX - 0.5).coerceIn(0.0, width - 1.0).toFloat(), (sourceY - 0.5).coerceIn(0.0, height - 1.0).toFloat()) ?: background
        }
    })
}

private fun averageDistance(a: PixelCoordinate, b: PixelCoordinate, c: PixelCoordinate, d: PixelCoordinate): Int {
    val first = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
    val second = hypot((c.x - d.x).toDouble(), (c.y - d.y).toDouble())
    val average = (first + second) / 2
    require(average.isFinite() && average <= Int.MAX_VALUE)
    return average.toInt().coerceAtLeast(1)
}
