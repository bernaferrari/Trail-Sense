package com.kylecorry.trail_sense.shared.io

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.graphics.scale
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.asImageBitmap
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.images.AppImages

/** [AppImages] over the app's file subsystem: assets and stored files decoded to bitmaps. */
class AndroidAppImages(private val files: FileSubsystem) : AppImages {

    override suspend fun load(path: String, maxWidth: Int?, maxHeight: Int?): ImageBitmap? = onIO {
        try {
            val bitmap = if (maxWidth != null && maxHeight != null) {
                files.bitmap(path, maxWidth, maxHeight)?.let { downscale(it, maxWidth, maxHeight) }
            } else {
                files.bitmap(path)
            }
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun asset(relativePath: String): String = files.SCHEME_ASSETS + relativePath

    /** Bundled assets decode at full size; shrink them so they still cover [maxWidth] x [maxHeight]. */
    private fun downscale(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = max(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
        if (scale >= 1f) {
            return bitmap
        }
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = bitmap.scale(width, height)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }
}
