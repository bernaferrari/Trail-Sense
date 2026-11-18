package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.shared.icons.trail.ArrowTarget
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import kotlin.math.ceil

internal val LocalLinearCompassAvailable = staticCompositionLocalOf { true }

/** The localized letters for the four cardinal directions, resolved by the caller. */
data class CardinalLabels(
    val north: String,
    val east: String,
    val south: String,
    val west: String
)

/**
 * A strip compass: the bearings within [range] degrees of [azimuth] laid out left to right, with the current heading
 * marked at the top center. Markers and bearing indicators are drawn at their bearings.
 */
@Composable
fun LinearCompass(
    azimuth: Float,
    labels: CardinalLabels,
    modifier: Modifier = Modifier,
    range: Float = 180f,
    markers: List<CompassMarker> = emptyList(),
    bearings: List<CompassBearingIndicator> = emptyList(),
    cardinalColor: Color = MaterialTheme.colorScheme.primary,
    tickColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val textMeasurer = rememberTextMeasurer()
    val headingPainter = rememberVectorPainter(TrailIcons.ArrowTarget)
    val painters = rememberIconPainters(markers.map { it.icon })
    val labelStyle = TextStyle(color = tickColor, fontSize = 15.sp)

    Canvas(modifier) {
        val iconSize = 25.dp.toPx()
        val toX = { bearing: Float ->
            Trigonometry.deltaAngle(azimuth, bearing) / range * size.width + size.width / 2f
        }

        // The heading
        drawIcon(headingPainter, Offset(size.width / 2f - iconSize / 2f, 0f), iconSize, tint = tickColor)

        // Ticks every 5 degrees, longer at 15 and 90, with letters at the cardinal directions
        val minimum = azimuth - range / 2
        val maximum = azimuth + range / 2
        var value = ceil(minimum / 5f) * 5f
        while (value <= maximum) {
            val degrees = value.toInt()
            val x = toX(value)
            val top = when {
                degrees % 90 == 0 -> 0.5f * size.height
                degrees % 15 == 0 -> 0.75f * size.height
                else -> 10 / 12f * size.height
            }
            drawLine(
                if (degrees % 45 == 0) cardinalColor else tickColor,
                Offset(x, size.height),
                Offset(x, top),
                strokeWidth = TICK_STROKE_WIDTH
            )
            if (degrees % 90 == 0) {
                val label = when (degrees.mod(360)) {
                    0 -> labels.north
                    90 -> labels.east
                    180 -> labels.south
                    else -> labels.west
                }
                val layout = textMeasurer.measure(label, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(x - layout.size.width / 2f, 5 / 12f * size.height - layout.firstBaseline)
                )
            }
            value += 5f
        }

        // Bearings fill from the center toward their direction
        bearings.forEach {
            val x = toX(it.bearing)
            drawRect(
                it.color.copy(alpha = BEARING_ALPHA),
                topLeft = Offset(minOf(size.width / 2f, x), size.height / 2f),
                size = Size(kotlin.math.abs(x - size.width / 2f), size.height / 2f)
            )
        }

        markers.forEach {
            val painter = painters[it.icon] ?: return@forEach
            val markerSize = it.size?.toPx() ?: iconSize
            val x = toX(it.bearing).coerceIn(0f, size.width)
            translate(x - markerSize / 2f, (iconSize - markerSize) * 0.6f) {
                rotate(it.rotation, pivot = Offset(markerSize / 2f, markerSize / 2f)) {
                    drawIcon(painter, Offset.Zero, markerSize, it.tint, it.opacity)
                }
            }
        }
    }
}

@Composable
internal fun rememberIconPainters(icons: List<ImageVector>): Map<ImageVector, VectorPainter> {
    val painters = mutableMapOf<ImageVector, VectorPainter>()
    icons.distinct().forEach { icon ->
        key(icon) {
            painters[icon] = rememberVectorPainter(icon)
        }
    }
    return painters
}

internal fun DrawScope.drawIcon(
    painter: VectorPainter,
    topLeft: Offset,
    size: Float,
    tint: Color? = null,
    alpha: Float = 1f
) {
    translate(topLeft.x, topLeft.y) {
        with(painter) {
            draw(
                Size(size, size),
                alpha = alpha,
                colorFilter = tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) }
            )
        }
    }
}

private const val TICK_STROKE_WIDTH = 8f
private const val BEARING_ALPHA = 100 / 255f
