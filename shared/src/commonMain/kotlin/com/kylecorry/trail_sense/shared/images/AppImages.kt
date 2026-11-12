package com.kylecorry.trail_sense.shared.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.kylecorry.trail_sense.main.getAppService

/** Decodes the app's images (bundled assets and stored files) for display; supplied by the platform. */
interface AppImages {
    /**
     * The image at [path] (a stored file's path, or [asset] for a bundled one), scaled to fit within [maxSize] pixels
     * when given. Null when it can't be read.
     */
    suspend fun load(path: String, maxSize: Int? = null): ImageBitmap?

    /** The path [load] takes for the bundled asset at [relativePath]. */
    fun asset(relativePath: String): String
}

/** The image at [path], loaded off the main thread; null while loading, when [path] is null, or when unreadable. */
@Composable
fun rememberAppImage(path: String?, maxSize: Int? = null): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, path, maxSize) {
        value = path?.let { getAppService<AppImages>().load(it, maxSize) }
    }
    return image
}
