package com.kylecorry.trail_sense.tools.navigation.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.trail_sense.shared.icons.toBitmap

/**
 * Loads icon bitmaps for a canvas. The [drawer] is kept so callers still tie a loader to the canvas it draws on.
 */
class DrawerBitmapLoader(@Suppress("unused") private val drawer: ICanvasDrawer) {

    private val icons = mutableMapOf<ImageVector, Bitmap>()
    private val lock = Any()

    fun load(icon: ImageVector, size: Int): Bitmap {
        return synchronized(lock) {
            icons.getOrPut(icon) { icon.toBitmap(size, size) }
        }
    }

    fun clear() {
        synchronized(lock) {
            icons.clear()
        }
    }

}
