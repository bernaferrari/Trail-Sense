package com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.operations

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.BitmapUtils.lut
import com.kylecorry.andromeda.bitmaps.LookupTable
import com.kylecorry.trail_sense.shared.andromeda_temp.bitmaps.operations.BitmapOperation

class Lut(private val table: LookupTable) : BitmapOperation {
    override fun execute(bitmap: Bitmap): Bitmap {
        return bitmap.lut(table)
    }
}