package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.ImageInterpolation
import com.kylecorry.andromeda.bitmaps.PixelImage
import com.kylecorry.andromeda.bitmaps.resizeToFit

enum class ImageResizeMode { Stretch, Fit, CenterCrop }

/** Resizes to model input dimensions, optionally preserving aspect ratio. */
fun PixelImage.resizeForModel(
    width: Int,
    height: Int,
    mode: ImageResizeMode = ImageResizeMode.Stretch,
    interpolation: ImageInterpolation = ImageInterpolation.Bilinear,
    background: Int = 0xff000000.toInt(),
): PixelImage {
    require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE)
    val source = this
    val result = when (mode) {
        ImageResizeMode.Stretch -> source.resize(width, height, interpolation)
        ImageResizeMode.Fit -> {
            val fitted = source.resizeToFit(width, height, interpolation)
            val horizontal = width - fitted.width
            val vertical = height - fitted.height
            fitted.pad(horizontal / 2, vertical / 2, horizontal - horizontal / 2, vertical - vertical / 2, background)
        }
        ImageResizeMode.CenterCrop -> {
            val cropWidth: Int
            val cropHeight: Int
            if (source.width.toLong() * height > source.height.toLong() * width) {
                cropWidth = (source.height.toLong() * width / height).toInt().coerceAtLeast(1)
                cropHeight = source.height
            } else {
                cropWidth = source.width
                cropHeight = (source.width.toLong() * height / width).toInt().coerceAtLeast(1)
            }
            source.crop((source.width - cropWidth) / 2, (source.height - cropHeight) / 2, cropWidth, cropHeight)
                .resize(width, height, interpolation)
        }
    }
    return result
}
