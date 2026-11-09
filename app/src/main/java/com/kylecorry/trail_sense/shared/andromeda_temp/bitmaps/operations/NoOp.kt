package com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.operations

import android.graphics.Bitmap

class NoOp : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap
    }
}