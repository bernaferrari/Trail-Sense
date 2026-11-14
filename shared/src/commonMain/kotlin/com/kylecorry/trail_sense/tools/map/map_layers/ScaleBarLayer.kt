package com.kylecorry.trail_sense.tools.map.map_layers

import com.kylecorry.trail_sense.shared.colors.ColorInts
import androidx.compose.ui.graphics.Path
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer
import com.kylecorry.trail_sense.tools.paths.ui.DistanceScale

// TODO: Allow position to be adjusted
class ScaleBarLayer(private val units: DistanceUnits, private val formatter: FormatService) : OverlayLayer() {

    override val layerId: String = LAYER_ID

    private val scaleBar = Path()
    private val distanceScale = DistanceScale()

    private val bottomMargin: Float = 32f

    override fun drawOverlay(
        drawer: MapDrawer,
        map: IMapView
    ) {
        drawer.noFill()

        val scaleSize =
            distanceScale.getScaleDistance(units, drawer.width / 2f, map.resolutionPixels)

        scaleBar.reset()
        distanceScale.getScaleBar(scaleSize, map.resolutionPixels, scaleBar)
        val start = drawer.width - drawer.dp(16f) - drawer.pathWidth(scaleBar)
        val y = drawer.height - drawer.dp(bottomMargin)
        drawer.push()
        drawer.translate(start, y)
        drawer.stroke(ColorInts.Black)
        drawer.strokeWeight(8f)
        drawer.path(scaleBar)
        drawer.stroke(ColorInts.White)
        drawer.strokeWeight(4f)
        drawer.path(scaleBar)
        drawer.pop()

        drawer.textMode(TextMode.Corner)
        drawer.textSize(drawer.sp(12f))
        drawer.strokeWeight(4f)
        drawer.stroke(ColorInts.Black)
        drawer.fill(ColorInts.White)
        val scaleText =
            formatter.formatDistance(scaleSize, Units.getDecimalPlaces(scaleSize.units), false)
        drawer.text(
            scaleText,
            start - drawer.textWidth(scaleText) - drawer.dp(4f),
            y + drawer.textHeight(scaleText) / 2
        )
    }

    companion object {
        const val LAYER_ID = "scale_bar"
    }
}
