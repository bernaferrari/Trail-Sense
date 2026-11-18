package com.kylecorry.trail_sense.tools.qr.infrastructure

import com.kylecorry.andromeda.bitmaps.decodeImage
import com.kylecorry.andromeda.qr.CodeDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun decodeQrFromFrame(width: Int, height: Int, argb: IntArray): String? = withContext(Dispatchers.Default) {
    if (width <= 0 || height <= 0 || width.toLong() * height > argb.size) {
        return@withContext null
    }
    CodeDecoder.qr(argb, width, height)
}

suspend fun decodeQrFromImageBytes(bytes: ByteArray): String? = withContext(Dispatchers.Default) {
    val image = runCatching { decodeImage(bytes) }.getOrNull() ?: return@withContext null
    CodeDecoder.qr(image.pixels, image.width, image.height)
}
