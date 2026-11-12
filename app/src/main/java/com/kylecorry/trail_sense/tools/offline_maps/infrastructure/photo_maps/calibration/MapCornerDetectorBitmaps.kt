package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.kylecorry.andromeda.bitmaps.operations.getPixels
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PixelBounds

/** Detects the map corners in a bitmap, in the bitmap's pixels. */
fun MapCornerDetector.detect(bitmap: Bitmap): PixelBounds? {
    val (width, height) = detectionSize(bitmap.width, bitmap.height) ?: return null
    val scaled = if (width != bitmap.width || height != bitmap.height) {
        bitmap.scale(width, height)
    } else {
        bitmap
    }
    return try {
        detect(scaled.getPixels(), scaled.width, scaled.height, bitmap.width, bitmap.height)
    } finally {
        if (scaled !== bitmap) {
            scaled.recycle()
        }
    }
}
