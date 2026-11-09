package com.kylecorry.andromeda.bitmaps.operations

import android.graphics.Bitmap

public interface BitmapOperation {
    public fun execute(bitmap: Bitmap): Bitmap
}