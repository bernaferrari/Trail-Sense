package com.kylecorry.andromeda.qr

import android.graphics.Bitmap

object Barcode {
    fun decode(image: Bitmap, highAccuracy: Boolean = false): String? {
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        return CodeDecoder.barcode(pixels, image.width, image.height, highAccuracy)
    }
}
