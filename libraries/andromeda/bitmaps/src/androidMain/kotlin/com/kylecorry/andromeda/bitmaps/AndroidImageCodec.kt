package com.kylecorry.andromeda.bitmaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

public actual fun decodeImage(bytes: ByteArray): PixelImage {
    val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)) { "Unable to decode image" }
    return try { bitmap.toPixelImage() } finally { bitmap.recycle() }
}

public actual fun PixelImage.encodePng(): ByteArray = encode(Bitmap.CompressFormat.PNG, 100)
public actual fun PixelImage.encodeJpeg(quality: Int): ByteArray {
    require(quality in 0..100)
    return encode(Bitmap.CompressFormat.JPEG, quality)
}

private fun PixelImage.encode(format: Bitmap.CompressFormat, quality: Int): ByteArray {
    val bitmap = toBitmap()
    return try {
        ByteArrayOutputStream().use { output ->
            check(bitmap.compress(format, quality, output)) { "Unable to encode image" }
            output.toByteArray()
        }
    } finally { bitmap.recycle() }
}
