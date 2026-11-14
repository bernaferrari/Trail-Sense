package com.kylecorry.andromeda.bitmaps

import android.graphics.Bitmap

public fun Bitmap.toPixelImage(): PixelImage {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    return PixelImage(width, height, pixels)
}

public fun PixelImage.toBitmap(): Bitmap = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
