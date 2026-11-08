package com.kylecorry.andromeda.print

import android.app.Activity
import android.content.Context
import androidx.print.PrintHelper
import com.kylecorry.andromeda.bitmaps.toBitmap

fun imagePrinter(context: Context): ImagePrinter = ImagePrinter { image, jobName ->
    require(context is Activity) { "Printing requires an Activity context" }
    val bitmap = image.toBitmap()
    try {
        PrintHelper(context).apply { scaleMode = PrintHelper.SCALE_MODE_FIT }
            .printBitmap(jobName, bitmap) { bitmap.recycle() }
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}
