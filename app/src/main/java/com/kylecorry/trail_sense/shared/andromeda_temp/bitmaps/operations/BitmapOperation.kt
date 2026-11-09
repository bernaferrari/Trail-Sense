package com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.operations

import android.graphics.Bitmap

interface BitmapOperation {
    fun execute(bitmap: Bitmap): Bitmap
}