package com.kylecorry.trail_sense.tools.map.ui

import com.kylecorry.trail_sense.shared.colors.ColorInts
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import kotlin.math.min

class MapMagnifier {

    private val clipPath = Path()

    fun draw(drawer: MapDrawer, touch: PixelCoordinate, drawMap: () -> Unit) {
        val width = drawer.width
        val size = min(width, drawer.height) / 4f
        val left = getLeft(drawer, touch, size)
        val center = PixelCoordinate(left + size / 2f, size / 2f)

        clipPath.reset()
        clipPath.addRect(Rect(left, 0f, left + size, size))

        drawer.push()
        drawer.clip(clipPath)
        drawer.background(ColorInts.Black)
        drawer.translate(center.x, center.y)
        drawer.scale(ZOOM)
        drawer.translate(-touch.x, -touch.y)
        drawMap()
        drawer.pop()

        drawer.noFill()
        drawer.stroke(ColorInts.White)
        drawer.strokeWeight(drawer.dp(OUTLINE_DP))
        drawer.path(clipPath)
    }

    private fun getLeft(drawer: MapDrawer, touch: PixelCoordinate, size: Float): Float {
        val width = drawer.width
        return if (touch.x > width / 2f) {
            0f
        } else {
            // Stay clear of the buttons along the right edge of the map
            width - drawer.dp(BUTTON_CLEARANCE_DP) - size
        }
    }

    companion object {
        private const val ZOOM = 2.5f
        private const val OUTLINE_DP = 2f
        private const val BUTTON_CLEARANCE_DP = 72f
    }
}
