package com.kylecorry.andromeda.bitmaps

import android.graphics.BitmapRegionDecoder
import android.graphics.Rect

public actual class ImageRegionDecoder private constructor(private val decoder: BitmapRegionDecoder) : AutoCloseable {
    @Suppress("DEPRECATION")
    public actual constructor(bytes: ByteArray) : this(requireNotNull(BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)) { "Unable to decode image" })

    @Suppress("DEPRECATION")
    public actual constructor(path: String) : this(requireNotNull(BitmapRegionDecoder.newInstance(path, false)) { "Unable to decode image" })
    private var closed = false
    public actual val width: Int = decoder.width
    public actual val height: Int = decoder.height

    public actual fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage {
        check(!closed) { "Image decoder is closed" }
        requireImageRegion(left, top, width, height, this.width, this.height)
        val bitmap = requireNotNull(decoder.decodeRegion(Rect(left, top, left + width, top + height), null)) { "Unable to decode image region" }
        return try { bitmap.toPixelImage() } finally { bitmap.recycle() }
    }

    actual override fun close() {
        if (!closed) { closed = true; decoder.recycle() }
    }
}
