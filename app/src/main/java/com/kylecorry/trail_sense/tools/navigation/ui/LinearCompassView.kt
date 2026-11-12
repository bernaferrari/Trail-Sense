package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.TrailSenseColors
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

/**
 * Adapts the compass layers to the shared [LinearCompass]: each frame the layers report their markers and bearings,
 * which are then drawn as state.
 */
class LinearCompassView(
    context: Context
) : BaseCompassView(context) {

    private val formatService = FormatService.getInstance(context)
    private val labels by lazy {
        CardinalLabels(
            north = formatService.formatDirection(CompassDirection.North),
            east = formatService.formatDirection(CompassDirection.East),
            south = formatService.formatDirection(CompassDirection.South),
            west = formatService.formatDirection(CompassDirection.West)
        )
    }

    private var markers = mutableListOf<CompassMarker>()
    private var bearings = mutableListOf<CompassBearingIndicator>()

    var range = 180f
        set(value) {
            field = value
            requestRedraw()
        }

    @Composable
    override fun Content(modifier: Modifier) {
        TrailSenseTheme {
            renderVersion
            val sized = modifier
                .fillMaxSize()
                .onSizeChanged { updateContentSize(it.width, it.height) }
            if (!isVisible) {
                Spacer(sized)
                return@TrailSenseTheme
            }
            markers = mutableListOf()
            bearings = mutableListOf()
            drawCompassLayers()
            LinearCompass(
                azimuth = azimuth,
                labels = labels,
                range = range,
                markers = markers,
                bearings = bearings,
                cardinalColor = Color(Resources.getCardinalDirectionColor(context)),
                tickColor = TrailSenseColors.current.onSurface,
                modifier = sized
            )
        }
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        markers.add(
            CompassMarker(
                bearing = reference.bearing,
                icon = reference.icon,
                tint = reference.tint?.let { Color(it) },
                opacity = reference.opacity,
                rotation = reference.rotation,
                size = size?.dp
            )
        )
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        bearings.add(CompassBearingIndicator(bearing.bearing, Color(bearing.color)))
    }
}
