package com.kylecorry.trail_sense.shared.images

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.kylecorry.luna.concurrency.onIO

/** Decodes pictures from the app's assets folder. */
class AndroidBundledImages(private val context: Context) : BundledImages {
    override suspend fun load(path: String): ImageBitmap? = onIO {
        runCatching {
            context.assets.open(path.removePrefix(ASSET_PREFIX)).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    private companion object {
        const val ASSET_PREFIX = "file:///android_asset/"
    }
}
