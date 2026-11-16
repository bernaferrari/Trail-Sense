package com.kylecorry.andromeda.bitmaps

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

public actual fun decodeImage(bytes: ByteArray): PixelImage {
    val image = runCatching { Image.makeFromEncoded(bytes) }.getOrNull()
        ?: throw IllegalArgumentException("Unable to decode image")
    return try {
        decodeRegion(image, 0, 0, image.width, image.height)
    } finally {
        image.close()
    }
}

public actual fun PixelImage.encodePng(): ByteArray = encode(EncodedImageFormat.PNG, 100)

public actual fun PixelImage.encodeJpeg(quality: Int): ByteArray {
    require(quality in 0..100)
    return encode(EncodedImageFormat.JPEG, quality)
}

private fun PixelImage.encode(format: EncodedImageFormat, quality: Int): ByteArray {
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    val bgra = ByteArray(pixels.size * 4)
    for (i in pixels.indices) {
        val color = pixels[i]
        val base = i * 4
        bgra[base] = (color and 0xFF).toByte()
        bgra[base + 1] = ((color ushr 8) and 0xFF).toByte()
        bgra[base + 2] = ((color ushr 16) and 0xFF).toByte()
        bgra[base + 3] = ((color ushr 24) and 0xFF).toByte()
    }
    val bitmap = Bitmap()
    return try {
        check(bitmap.allocPixels(info)) { "Unable to allocate bitmap" }
        check(bitmap.installPixels(info, bgra, width * 4)) { "Unable to install bitmap pixels" }
        val image = Image.makeFromBitmap(bitmap)
        try {
            val data = image.encodeToData(format, quality) ?: error("Unable to encode image")
            try {
                data.bytes
            } finally {
                data.close()
            }
        } finally {
            image.close()
        }
    } finally {
        bitmap.close()
    }
}

private fun decodeRegion(image: Image, left: Int, top: Int, width: Int, height: Int): PixelImage {
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    val bitmap = Bitmap()
    return try {
        check(bitmap.allocPixels(info)) { "Unable to allocate bitmap" }
        check(image.readPixels(bitmap, left, top)) { "Unable to read image pixels" }
        val raw = requireNotNull(bitmap.readPixels(info, width * 4, 0, 0)) { "Unable to extract pixels" }
        val pixels = IntArray(width * height) { index ->
            val base = index * 4
            val b = raw[base].toInt() and 0xFF
            val g = raw[base + 1].toInt() and 0xFF
            val r = raw[base + 2].toInt() and 0xFF
            val a = raw[base + 3].toInt() and 0xFF
            (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        PixelImage(width, height, pixels)
    } finally {
        bitmap.close()
    }
}

public actual class ImageRegionDecoder private constructor(private val image: Image) : AutoCloseable {
    public actual constructor(bytes: ByteArray) : this(
        runCatching { Image.makeFromEncoded(bytes) }.getOrNull()
            ?: throw IllegalArgumentException("Unable to decode image")
    )

    public actual constructor(path: String) : this(unsupportedPath(path))

    private var closed = false
    public actual val width: Int = image.width
    public actual val height: Int = image.height

    public actual fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage {
        check(!closed) { "Image decoder is closed" }
        requireImageRegion(left, top, width, height, this.width, this.height)
        return decodeRegion(image, left, top, width, height)
    }

    actual override fun close() {
        if (!closed) {
            closed = true
            image.close()
        }
    }

    private companion object {
        private fun unsupportedPath(path: String): Image {
            throw UnsupportedOperationException("File path image decoding is unavailable on Web: $path")
        }
    }
}
