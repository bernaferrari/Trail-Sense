package com.kylecorry.trail_sense.tools.map.map_layers

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.colors.ColorInts
import android.os.Bundle
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.icons.toImageBitmap
import com.kylecorry.trail_sense.tools.sensors.SensorsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class MyElevationLayer : OverlayLayer() {

    private val bottomLeft = PixelCoordinate(
        16f,
        -16f
    )

    private val formatter = DependencyRegistry.get<FormatService>()
    private val prefs = DependencyRegistry.get<UserPreferences>()
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()

    override val layerId: String = LAYER_ID

    var elevation = Distance.meters(0f)
        set(value) {
            field = value
            elevationString = formatter.formatDistance(value)
        }

    private var elevationString = ""

    override fun start() {
        Tools.subscribe(SensorsToolRegistration.BROADCAST_ELEVATION_CHANGED, this::onElevationChange)
    }

    override fun drawOverlay(
        drawer: MapDrawer,
        map: IMapView
    ) {
        val elevationIcon = TrailIcons.Altitude.toImageBitmap(drawer.sp(20f).toInt())

        drawer.push()
        drawer.translate(drawer.dp(bottomLeft.x), drawer.height + drawer.dp(bottomLeft.y))
        drawer.textMode(TextMode.Corner)
        drawer.textSize(drawer.sp(12f))
        drawer.strokeWeight(4f)
        drawer.stroke(ColorInts.Black)
        drawer.fill(ColorInts.White)

        drawer.tint(ColorInts.Black)
        drawer.imageMode(ImageMode.Corner)
        drawer.image(elevationIcon, 0f, -elevationIcon.height / 2f)
        drawer.tint(ColorInts.White)
        drawer.image(
            elevationIcon,
            4f,
            -elevationIcon.height / 2f + 4f,
            elevationIcon.width.toFloat() - 8f,
            elevationIcon.height.toFloat() - 8f
        )
        drawer.noTint()

        drawer.text(
            elevationString,
            elevationIcon.width + drawer.dp(4f),
            drawer.textHeight(elevationString) / 2
        )
        drawer.pop()
    }

    override fun stop() {
        Tools.unsubscribe(SensorsToolRegistration.BROADCAST_ELEVATION_CHANGED, this::onElevationChange)
    }

    private fun onElevationChange(ignored: Bundle) {
        elevation = locationSubsystem.elevation.convertTo(prefs.baseDistanceUnits)
    }


    companion object {
        const val LAYER_ID = "my_elevation"
    }
}
