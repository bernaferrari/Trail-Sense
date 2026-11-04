package com.kylecorry.trail_sense.tools.navigation.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.toBitmap

class BitmapLoader {

    private val icons = mutableMapOf<Pair<ImageVector, Int>, Bitmap>()

    fun load(icon: ImageVector, size: Int): Bitmap {
        return icons.getOrPut(icon to size) { icon.toBitmap(size, size) }
    }

    fun unload(icon: ImageVector) {
        // The bitmaps are shared with the vector bitmap cache, so they are released rather than recycled
        icons.keys.filter { it.first == icon }.forEach { icons.remove(it) }
    }

    fun clear() {
        icons.clear()
    }

}
