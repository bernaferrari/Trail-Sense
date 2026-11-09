package com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.operations

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.BitmapUtils.crop
import com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.operations.BitmapOperation

class Crop(
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float
) : BitmapOperation {

    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap.crop(x, y, width, height)
    }
}