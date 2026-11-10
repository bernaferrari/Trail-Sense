package com.kylecorry.andromeda.bitmaps.operations

import android.graphics.Bitmap

public class NoOp : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap
    }
}