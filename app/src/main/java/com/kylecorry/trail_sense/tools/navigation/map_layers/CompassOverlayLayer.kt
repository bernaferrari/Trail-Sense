package com.kylecorry.trail_sense.tools.navigation.map_layers

import android.content.Context
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer

// TODO: Allow setting of position
class CompassOverlayLayer : OverlayLayer() {

    override val layerId: String = LAYER_ID

    private var backgroundColor: Int? = null
    private var cardinalDirectionColor: Int? = null

    var paddingTopDp: Float = 8f
        set(value) {
            field = value
            invalidate()
        }

    var paddingRightDp: Float = 8f
        set(value) {
            field = value
            invalidate()
        }

    override fun drawOverlay(
        context: Context,
        drawer: MapDrawer,
        map: IMapView
    ) {
        if (backgroundColor == null) {
            backgroundColor = Resources.color(context, R.color.colorSecondary)
        }

        if (cardinalDirectionColor == null) {
            cardinalDirectionColor = Resources.getCardinalDirectionColor(context)
        }

        val compassSize = drawer.dp(24f)
        val arrowWidth = drawer.dp(5f)
        val arrowMargin = drawer.dp(3f)
        val location = com.kylecorry.andromeda.core.units.PixelCoordinate(
            drawer.width - drawer.dp(paddingRightDp + compassSize / 2f),
            drawer.dp(paddingTopDp + compassSize / 2f)
        )
        drawer.push()
        drawer.rotate(-map.mapAzimuth, location.x, location.y)

        // Background circle
        drawer.noTint()
        drawer.fill(backgroundColor ?: ColorInts.White)
        drawer.stroke(ColorInts.White)
        drawer.strokeWeight(drawer.dp(1f))
        drawer.circle(location.x, location.y, compassSize)

        // Top triangle
        drawer.noStroke()
        drawer.fill(cardinalDirectionColor ?: ColorInts.White)
        drawer.triangle(
            location.x,
            location.y - compassSize / 2f + arrowMargin,
            location.x - arrowWidth / 2f,
            location.y,
            location.x + arrowWidth / 2f,
            location.y
        )

        // Bottom triangle
        drawer.fill(ColorInts.White)
        drawer.triangle(
            location.x,
            location.y + compassSize / 2f - arrowMargin,
            location.x - arrowWidth / 2f,
            location.y,
            location.x + arrowWidth / 2f,
            location.y
        )

        drawer.pop()
    }

    companion object {
        const val LAYER_ID = "compass_overlay"
    }
}
