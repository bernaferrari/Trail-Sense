package com.kylecorry.andromeda.bitmaps

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

/** Rotates clockwise in screen coordinates, expanding the result to contain the image. */
public fun PixelImage.rotate(degrees: Float, background: Int = 0): PixelImage {
    require(degrees.isFinite())
    val radians = (degrees % 360).toDouble() * PI / 180
    val cosine = cos(radians).let { if (abs(it) < 1e-12) 0.0 else it }
    val sine = sin(radians).let { if (abs(it) < 1e-12) 0.0 else it }
    val rotatedWidth = ceil(abs(width * cosine) + abs(height * sine)).toLong()
    val rotatedHeight = ceil(abs(width * sine) + abs(height * cosine)).toLong()
    require(rotatedWidth in 1..Int.MAX_VALUE && rotatedHeight in 1..Int.MAX_VALUE && rotatedWidth * rotatedHeight <= Int.MAX_VALUE)
    val outputWidth = rotatedWidth.toInt()
    val outputHeight = rotatedHeight.toInt()
    return PixelImage(outputWidth, outputHeight, IntArray(outputWidth * outputHeight) { index ->
        val x = index % outputWidth + 0.5 - outputWidth / 2.0
        val y = index / outputWidth + 0.5 - outputHeight / 2.0
        val sourceX = cosine * x + sine * y + width / 2.0
        val sourceY = -sine * x + cosine * y + height / 2.0
        if (sourceX < 0 || sourceX >= width || sourceY < 0 || sourceY >= height) background
        else interpolate((sourceX - 0.5).coerceIn(0.0, width - 1.0).toFloat(), (sourceY - 0.5).coerceIn(0.0, height - 1.0).toFloat()) ?: background
    })
}
