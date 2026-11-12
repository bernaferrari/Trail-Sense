package com.kylecorry.trail_sense.shared.images

import androidx.compose.ui.graphics.ImageBitmap

/** Pictures bundled with the app, such as the ones the guides show. */
interface BundledImages {
    /**
     * The picture at [path], relative to the bundled assets (an Android `file:///android_asset/` prefix is allowed),
     * or null if it can't be read.
     */
    suspend fun load(path: String): ImageBitmap?
}
