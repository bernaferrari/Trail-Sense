package com.kylecorry.andromeda.bitmaps

import com.kylecorry.sol.math.trigonometry.Trigonometry.normalizeAngle
import com.kylecorry.sol.science.geology.CoordinateBounds
import kotlin.math.min
import kotlin.math.roundToInt

/** Resizes to fit inside the given bounds while preserving aspect ratio. */
public fun PixelImage.resizeToFit(
    maxWidth: Int,
    maxHeight: Int,
    interpolation: ImageInterpolation = ImageInterpolation.Bilinear,
    allowUpscale: Boolean = true,
): PixelImage {
    require(maxWidth > 0 && maxHeight > 0)
    val scale = min(maxWidth.toDouble() / width, maxHeight.toDouble() / height).let { if (allowUpscale) it else min(it, 1.0) }
    val newWidth = (width * scale).toInt().coerceAtLeast(1)
    val newHeight = (height * scale).toInt().coerceAtLeast(1)
    return resize(newWidth, newHeight, interpolation)
}

/** Samples a geographic tile from a rectangular equirectangular image, including antimeridian crossings. */
public fun PixelImage.cropTile(
    imageBounds: CoordinateBounds,
    tileBounds: CoordinateBounds,
    tileWidth: Int,
    tileHeight: Int,
    interpolation: ImageInterpolation = ImageInterpolation.Bilinear,
    background: Int = 0,
): PixelImage {
    require(tileWidth > 0 && tileHeight > 0 && tileWidth.toLong() * tileHeight <= Int.MAX_VALUE)
    val imageWidth = imageBounds.widthDegrees()
    val imageHeight = imageBounds.heightDegrees()
    val tileWidthDegrees = tileBounds.widthDegrees()
    val tileHeightDegrees = tileBounds.heightDegrees()
    require(imageWidth.isFinite() && imageHeight.isFinite() && imageWidth > 0 && imageHeight > 0)
    require(tileWidthDegrees.isFinite() && tileHeightDegrees.isFinite() && tileWidthDegrees > 0 && tileHeightDegrees > 0)
    val originY = (imageBounds.north - tileBounds.north) / imageHeight * height
    val regionHeight = tileHeightDegrees / imageHeight * height
    return PixelImage(tileWidth, tileHeight, IntArray(tileWidth * tileHeight) { index ->
        val longitude = tileBounds.west + (index % tileWidth + 0.5) * tileWidthDegrees / tileWidth
        val x = normalizeAngle(longitude - imageBounds.west) / imageWidth * width - 0.5
        val y = originY + (index / tileWidth + 0.5) * regionHeight / tileHeight - 0.5
        if (!x.isFinite() || !y.isFinite() || x < -0.5 || y < -0.5 || x >= width - 0.5 || y >= height - 0.5) background
        else {
            val px = x.coerceIn(0.0, width - 1.0)
            val py = y.coerceIn(0.0, height - 1.0)
            when (interpolation) {
                ImageInterpolation.Nearest -> this[px.roundToInt(), py.roundToInt()]
                ImageInterpolation.Bilinear -> interpolate(px.toFloat(), py.toFloat())!!
                ImageInterpolation.Bicubic -> interpolateBicubic(px, py)
            }
        }
    })
}
