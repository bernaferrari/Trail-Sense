package com.kylecorry.trail_sense.tools.navigation.ui.markers

import androidx.compose.ui.graphics.ImageBitmap
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

class BitmapMapMarker(
    override val location: Coordinate?,
    private val bitmap: ImageBitmap,
    override val size: Float = 12f,
    override val rotation: Float? = null,
    override val rotateWithUser: Boolean = false,
    override val scaleToLocationAccuracy: Boolean = false,
    private val tint: Int? = null,
    private val onClickFn: () -> Boolean = { false }
) : MapMarker {
    override fun draw(
        drawer: MapDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float,
        metersPerPixel: Float,
    ) {
        val size = drawer.dp(this.size) * scale
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        var finalWidth = size
        var finalHeight = size
        if (aspectRatio < 1f) {
            finalWidth = size * aspectRatio
        } else {
            finalHeight = size / aspectRatio
        }
        drawer.imageMode(ImageMode.Center)
        drawer.push()
        if (tint != null) {
            drawer.tint(tint)
        } else {
            drawer.noTint()
        }
        drawer.translate(anchor.x, anchor.y)
        drawer.rotate(this.rotation ?: rotation, 0f, 0f)
        drawer.image(bitmap, 0f, 0f, finalWidth, finalHeight)
        drawer.pop()
        drawer.imageMode(ImageMode.Corner)
        drawer.noTint()
    }

    override fun onClick(): Boolean {
        return onClickFn()
    }

    override fun calculateSizeInPixels(
        drawer: MapDrawer,
        metersPerPixel: Float,
        scale: Float
    ): Float {
        return drawer.dp(size) * scale
    }
}
