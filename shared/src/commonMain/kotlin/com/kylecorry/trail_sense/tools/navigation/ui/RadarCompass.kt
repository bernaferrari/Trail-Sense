package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.trail_sense.shared.icons.trail.ArrowTarget
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.trailSenseFont
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/** Something the radar draws, in the order its layers reported it. */
sealed interface RadarCompassItem {
    data class Mark(val marker: CompassMarker) : RadarCompassItem

    /**
     * The turn still to make toward [bearing]. When the destination is close, [stopDistance] is how far it is from the
     * dial's center in pixels, and the turn's shadow stops there.
     */
    data class Turn(val bearing: Float, val hasStop: Boolean, val stopDistance: Float? = null) : RadarCompassItem
}

/** Where the radar's parts sit for a canvas of the given size, in pixels. */
class RadarCompassGeometry(width: Float, height: Float, density: Density) {
    private val iconSize = (24 * density.density).toInt()

    /** The diameter the radar map is scaled to. */
    val compassSize: Int = min(height, width).toInt() - 2 * iconSize - 2 * (2 * density.density).toInt()

    /** The dial's outer radius: the whole canvas, so marks ride on its outer edge. */
    val outerRadius: Float = min(width, height) / 2f - iconSize * 0.55f

    /** The wavy dial's peak radius. */
    val dialRadius: Float = outerRadius * WAVY_RADIUS

    /** Map layers and the turn stay inside this radius, clear of the graduations. */
    val clipRadius: Float = dialRadius - 30 * density.density

    /** The middle holds the heading, so map layers keep out of this radius. */
    val headingClearRadius: Float = compassSize * HEADING_CLEAR_FRACTION
}

/**
 * The navigator's dial: a lobed disc in the primary container that turns with you, its graduations following the wavy
 * edge, which flattens into a circle as you line up with a cardinal direction. The letters sit outside it, upright.
 * Destinations point in from just outside the dial, and the turn still to make is a bold arc on its edge.
 * [showHeadingMark] adds the lubber line, fixed to the phone at the top of the dial.
 */
@Composable
fun RadarCompass(
    azimuth: Float,
    labels: CardinalLabels,
    modifier: Modifier = Modifier,
    items: List<RadarCompassItem> = emptyList(),
    lobes: Int = 12,
    showDial: Boolean = true,
    showHeadingMark: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val primary = colors.primary
    val text = colors.onSurface
    val turnColor = colors.tertiary
    val surface = colors.surface
    val hub = colors.primaryContainer
    val onHub = colors.onPrimaryContainer

    val textMeasurer = rememberTextMeasurer()
    val letterFont = trailSenseFont()
    val painters = rememberIconPainters(
        items.mapNotNull { (it as? RadarCompassItem.Mark)?.marker?.icon }
    )

    // Lined up with a cardinal, the dial eases from waves into a circle.
    val toCardinal = abs(deltaAngle(azimuth, (azimuth / 90f).roundToInt() * 90f))
    val flat by animateFloatAsState((1f - toCardinal / DETENT_DEGREES).coerceIn(0f, 1f))

    Canvas(modifier) {
        val geometry = RadarCompassGeometry(size.width, size.height, this)
        val dial = WavyDial(geometry.dialRadius, lobes, flat)
        rotate(-azimuth, center) {
            if (showDial) {
                drawWavyDial(dial, onHub, hub)
                val cardinalSize = 18.sp.toPx()
                val letterRadius = geometry.dialRadius + px(6f) + cardinalSize * 0.6f
                listOf(0f to labels.north, 90f to labels.east, 180f to labels.south, 270f to labels.west)
                    .forEach { (degrees, label) ->
                        val position = center + polar(degrees, letterRadius)
                        val layout = textMeasurer.measure(
                            label,
                            TextStyle(
                                color = if (degrees == 0f) primary else text,
                                fontSize = 18.sp * 1.1f,
                                fontFamily = letterFont,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        // Upright on screen: undo the dial's rotation around the letter itself.
                        rotate(azimuth, position) {
                            drawText(
                                layout,
                                topLeft = Offset(
                                    position.x - layout.size.width / 2f,
                                    position.y + cardinalSize * 0.4f - layout.firstBaseline
                                )
                            )
                        }
                    }
            }
            items.forEach { item ->
                when (item) {
                    is RadarCompassItem.Mark -> drawMark(item.marker, geometry, turnColor, painters)
                    is RadarCompassItem.Turn -> drawTurn(item, geometry, azimuth, turnColor)
                }
            }
        }
        if (showDial && showHeadingMark) {
            // Fixed to the phone: a short rounded pill crossing the top of the dial, turning primary at the detent.
            val top = center.y - dial.radiusAt(0f, flat = 1f)
            val start = Offset(center.x, top - px(8f))
            val end = Offset(center.x, top + px(14f))
            drawLine(surface, start, end, strokeWidth = px(9f), cap = StrokeCap.Round)
            drawLine(lerp(text, primary, flat), start, end, strokeWidth = px(5f), cap = StrokeCap.Round)
        }
    }
}

/** The dial's outline: [lobes] soft waves peaking on the major graduations, flattening with [flat]. */
private class WavyDial(private val radius: Float, val lobes: Int, private val flat: Float) {
    fun radiusAt(degrees: Float, flat: Float = this.flat): Float {
        val amplitude = WAVY_AMPLITUDE * (1f - flat)
        val wave = (1f - cos((degrees * lobes).toRadians())) / 2f
        return radius * (1f - amplitude * wave)
    }
}

private fun DrawScope.drawWavyDial(dial: WavyDial, ink: Color, fill: Color) {
    val shape = Path()
    for (degrees in 0..360) {
        val point = center + polar(degrees.toFloat(), dial.radiusAt(degrees.toFloat()))
        if (degrees == 0) shape.moveTo(point.x, point.y) else shape.lineTo(point.x, point.y)
    }
    shape.close()
    drawPath(shape, fill)

    // Graduations inside, hugging the edge.
    val step = 360 / dial.lobes
    for (degrees in 0 until 360 step 5) {
        val cardinal = degrees % 90 == 0
        val major = degrees % step == 0
        val mid = degrees % 15 == 0
        val alpha = when {
            cardinal -> 255
            major -> 210
            mid -> 130
            else -> 80
        } / 255f
        val width = px(when {
            cardinal -> 4.5f
            major -> 3f
            mid -> 2f
            else -> 1.5f
        })
        val length = px(when {
            cardinal -> 18f
            major -> 13f
            mid -> 9f
            else -> 6f
        })
        val start = dial.radiusAt(degrees.toFloat()) - px(9f)
        drawLine(
            ink.copy(alpha = alpha),
            center + polar(degrees.toFloat(), start),
            center + polar(degrees.toFloat(), start - length),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawMark(
    marker: CompassMarker,
    geometry: RadarCompassGeometry,
    turnColor: Color,
    painters: Map<androidx.compose.ui.graphics.vector.ImageVector, androidx.compose.ui.graphics.vector.VectorPainter>
) {
    if (marker.opacity == 0f) {
        return
    }
    val isTarget = marker.icon == TrailIcons.ArrowTarget
    // The dial shows destinations and tinted marks (beacons); the untinted sky art stays off it.
    if (!isTarget && marker.tint == null) {
        return
    }
    rotate(marker.bearing, center) {
        if (isTarget) {
            // Destinations sit just outside the dial, pointing in at their bearing: a solid rounded wedge in the
            // beacon's colour (tertiary for a plain bearing), or the beacon's chosen shape.
            val color = (marker.tint ?: turnColor).copy(alpha = marker.opacity)
            translate(center.x, center.y - (geometry.dialRadius + px(12f))) {
                val shape = marker.shape
                if (shape != null) {
                    drawPath(shape.toComposePath(px(20f)), color)
                } else {
                    val w = px(11f)
                    val h = px(13f)
                    val wedge = Path().apply {
                        moveTo(0f, h / 2f)
                        lineTo(-w, -h / 2f)
                        lineTo(w, -h / 2f)
                        close()
                    }
                    drawIntoCanvas {
                        it.drawPath(wedge, Paint().apply {
                            this.color = color
                            style = PaintingStyle.Fill
                            pathEffect = PathEffect.cornerPathEffect(px(3f))
                        })
                    }
                }
            }
        } else {
            val painter = painters[marker.icon] ?: return@rotate
            val side = marker.size?.toPx() ?: px(24f)
            translate(center.x, center.y - geometry.outerRadius * MARK_RING) {
                rotate(marker.rotation, Offset.Zero) {
                    drawIcon(painter, Offset(-side / 2f, -side / 2f), side, marker.tint, marker.opacity)
                }
            }
        }
    }
}

private fun DrawScope.drawTurn(
    turn: RadarCompassItem.Turn,
    geometry: RadarCompassGeometry,
    azimuth: Float,
    color: Color
) {
    val sweep = deltaAngle(azimuth, turn.bearing)
    val startAngle = azimuth - 90
    fun wedge(radius: Float, alpha: Int) = drawArc(
        color.copy(alpha = alpha / 255f),
        startAngle,
        sweep,
        useCenter = true,
        topLeft = center - Offset(radius, radius),
        size = Size(radius * 2, radius * 2)
    )

    wedge(geometry.clipRadius, if (turn.hasStop) 18 else 40)

    // The turn still to make, as a bold rounded arc on the dial's edge: the wedge is only its soft shadow.
    if (abs(sweep) > 0.5f) {
        val stroke = px(7f)
        val radius = geometry.clipRadius - stroke / 2f
        // The rounded caps end exactly on the wedge's edges instead of overhanging them.
        val capDegrees = (stroke / 2f / radius).toDegrees()
        if (abs(sweep) > capDegrees * 2) {
            val direction = sign(sweep)
            drawArc(
                color,
                startAngle + direction * capDegrees,
                sweep - direction * capDegrees * 2,
                useCenter = false,
                topLeft = center - Offset(radius, radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
    }

    turn.stopDistance?.let {
        wedge(min(geometry.compassSize.toFloat(), it * 2) / 2f, 40)
    }
}

/** A point [radius] from the origin at a compass bearing, where 0 is up. */
private fun polar(degrees: Float, radius: Float): Offset {
    val radians = (degrees - 90).toRadians()
    return Offset(cos(radians) * radius, sin(radians) * radius)
}

/** The polygon (in a unit square) as a path [side] pixels across, centered on the origin. */
private fun RoundedPolygon.toComposePath(side: Float): Path {
    val path = Path()
    cubics.forEachIndexed { index, cubic ->
        if (index == 0) {
            path.moveTo(cubic.anchor0X * side - side / 2f, cubic.anchor0Y * side - side / 2f)
        }
        path.cubicTo(
            cubic.control0X * side - side / 2f, cubic.control0Y * side - side / 2f,
            cubic.control1X * side - side / 2f, cubic.control1Y * side - side / 2f,
            cubic.anchor1X * side - side / 2f, cubic.anchor1Y * side - side / 2f
        )
    }
    path.close()
    return path
}

private fun DrawScope.px(dp: Float): Float = dp * density

private const val WAVY_RADIUS = 0.87f
private const val WAVY_AMPLITUDE = 0.05f
private const val DETENT_DEGREES = 6f
private const val MARK_RING = 0.62f
private const val HEADING_CLEAR_FRACTION = 0.2f
