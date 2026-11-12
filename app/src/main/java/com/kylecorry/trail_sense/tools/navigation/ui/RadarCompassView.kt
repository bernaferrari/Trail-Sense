package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Path
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.map.ui.MapView

/**
 * Adapts the compass layers to the shared [RadarCompass]: each frame the layers report their marks and turns, which are
 * then drawn as state. It also keeps the radar map clipped to the inside of the dial.
 */
class RadarCompassView(
    context: Context
) : BaseCompassView(context) {

    private val labels = CardinalLabels(
        north = context.getString(R.string.direction_north),
        east = context.getString(R.string.direction_east),
        south = context.getString(R.string.direction_south),
        west = context.getString(R.string.direction_west)
    )
    private val lobes = prefs.navigation.dialLobes
    private var mapView: MapView? = null
    private var density: Density? = null
    private var items = mutableListOf<RadarCompassItem>()

    var shouldDrawDial: Boolean = true
        set(value) {
            field = value
            requestRedraw()
        }

    /** Whether the dial shows the lubber line, the mark fixed to the phone that the heading is read against. */
    var shouldDrawAzimuthIndicator: Boolean = true
        set(value) {
            field = value
            requestRedraw()
        }

    @Composable
    override fun Content(modifier: Modifier) {
        val currentDensity = LocalDensity.current
        val sized = modifier
            .fillMaxSize()
            .onSizeChanged {
                density = currentDensity
                updateContentSize(it.width, it.height)
                updateClip()
            }
        renderVersion
        if (!isVisible) {
            Spacer(sized)
            return
        }
        items = mutableListOf()
        drawCompassLayers()
        RadarCompass(
            azimuth = azimuth,
            labels = labels,
            items = items,
            lobes = lobes,
            showDial = shouldDrawDial,
            showHeadingMark = shouldDrawAzimuthIndicator,
            modifier = sized
        )
    }

    fun bindMapView(view: MapView) {
        mapView = view
        updateClip()
        requestRedraw()
    }

    private fun geometry(): RadarCompassGeometry? {
        val density = density ?: return null
        if (width <= 0 || height <= 0) {
            return null
        }
        return RadarCompassGeometry(width.toFloat(), height.toFloat(), density)
    }

    /** The radar's paths draw only inside the dial's graduations, and keep out of the heading in the middle. */
    private fun updateClip() {
        val map = mapView ?: return
        val geometry = geometry() ?: return
        val centerX = width / 2f
        val centerY = height / 2f
        map.clipPath = Path().apply {
            addCircle(centerX, centerY, geometry.clipRadius, Path.Direction.CW)
            addCircle(centerX, centerY, geometry.headingClearRadius, Path.Direction.CW)
            fillType = Path.FillType.EVEN_ODD
        }
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        items.add(
            RadarCompassItem.Mark(
                CompassMarker(
                    bearing = reference.bearing,
                    icon = reference.icon,
                    tint = reference.tint?.let { Color(it) },
                    opacity = reference.opacity,
                    rotation = reference.rotation,
                    size = size?.dp,
                    shape = reference.shape
                )
            )
        )
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        val center = PixelCoordinate(width / 2f, height / 2f)
        val stopDistance = stopAt?.let { mapView?.toPixel(it)?.distanceTo(center) }
        items.add(RadarCompassItem.Turn(bearing.bearing, stopAt != null, stopDistance))
    }
}
