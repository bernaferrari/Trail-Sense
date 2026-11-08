package com.kylecorry.andromeda.bitmaps

import android.graphics.BitmapRegionDecoder
import android.graphics.Rect

actual class ImageRegionDecoder private constructor(private val decoder: BitmapRegionDecoder) : AutoCloseable {
    @Suppress("DEPRECATION")
    actual constructor(bytes: ByteArray) : this(requireNotNull(BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)) { "Unable to decode image" })

    @Suppress("DEPRECATION")
    actual constructor(path: String) : this(requireNotNull(BitmapRegionDecoder.newInstance(path, false)) { "Unable to decode image" })
    private var closed = false
    actual val width: Int = decoder.width
    actual val height: Int = decoder.height

    actual fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage {
        check(!closed) { "Image decoder is closed" }
        requireImageRegion(left, top, width, height, this.width, this.height)
        val bitmap = requireNotNull(decoder.decodeRegion(Rect(left, top, left + width, top + height), null)) { "Unable to decode image region" }
        return try { bitmap.toPixelImage() } finally { bitmap.recycle() }
    }

    actual override fun close() {
        if (!closed) { closed = true; decoder.recycle() }
    }
}
