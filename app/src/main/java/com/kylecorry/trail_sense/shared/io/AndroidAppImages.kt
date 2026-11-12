package com.kylecorry.trail_sense.shared.io

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.images.AppImages

/** [AppImages] over the app's file subsystem: assets and stored files decoded to bitmaps. */
class AndroidAppImages(private val files: FileSubsystem) : AppImages {

    override suspend fun load(path: String, maxSize: Int?): ImageBitmap? = onIO {
        try {
            val bitmap = if (maxSize != null) files.bitmap(path, maxSize, maxSize) else files.bitmap(path)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun asset(relativePath: String): String = files.SCHEME_ASSETS + relativePath
}
