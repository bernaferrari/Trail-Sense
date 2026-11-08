package com.kylecorry.andromeda.bitmaps

/** Decodes an image supported by the platform into straight-alpha ARGB pixels. */
expect fun decodeImage(bytes: ByteArray): PixelImage

/** Encodes the image as a PNG. */
expect fun PixelImage.encodePng(): ByteArray

/** Encodes the image as a JPEG, with quality from 0 through 100. */
expect fun PixelImage.encodeJpeg(quality: Int = 90): ByteArray
