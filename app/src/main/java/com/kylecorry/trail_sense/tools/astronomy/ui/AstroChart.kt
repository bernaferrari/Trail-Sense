package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartFrame
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartGutter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartInset
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSmoothPath
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

/**
 * What the sun and moon chart shows: the charted day's altitudes ([sun], [moon], from [startTime] over 24 hours),
 * where each body is now ([sunPosition], [moonPosition], or null when another day is charted), and whether the
 * twilight limits are drawn ([showBands]).
 */
internal data class AstroChartState(
    val startTime: Instant = Instant.now(),
    val sun: List<Reading<Float>> = emptyList(),
    val moon: List<Reading<Float>> = emptyList(),
    val sunPosition: Reading<Float>? = null,
    val moonPosition: Reading<Float>? = null,
    val showBands: Boolean = false
)

/**
 * The sun and moon chart in the frame style: Above / Horizon / Below in the gutter, the sun as the one primary line,
 * the moon a quiet dotted line, the horizon (and twilight limits when asked for) as dashed limits, and the sun's place
 * as the "now" dot. Daylight (the sun's curve above the horizon) is one soft primaryContainer lake.
 *
 * [moonImage] is the moon's phase disc (24 dp; a plain phase bitmap recoloured here); without one the full moon icon
 * stands in. A tap on the sun or moon reports null, a tap elsewhere reports the time under it.
 */
@Composable
internal fun AstroChart(
    state: AstroChartState,
    moonImage: ImageBitmap?,
    modifier: Modifier = Modifier,
    onChartClicked: (time: ZonedDateTime?) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val sunColor = scheme.primary
    val daylightColor = scheme.primary.copy(alpha = 0.32f)
    val isDark = scheme.surface.luminance() < 0.5f
    val nightColor = if (isDark) NightSky.copy(alpha = 0.45f) else NightSky.copy(alpha = 0.07f)
    val moonColor = scheme.outline
    val limitColor = scheme.outlineVariant
    val horizonColor = scheme.outline.copy(alpha = 0.8f)
    val ringColor = scheme.background
    val moonOutline = scheme.outlineVariant
    val moonShadow = scheme.surfaceContainerHighest
    val moonFilter = remember(moonShadow) {
        ColorFilter.colorMatrix(ColorMatrix(moonPhaseColorMatrix(moonShadow, MoonLit)))
    }
    val fallbackMoon = rememberVectorPainter(TrailIcons.Moon)
    val onClick by rememberUpdatedState(onChartClicked)

    val sunLine = remember(state.startTime, state.sun) { state.sun.map { it.toVector(state.startTime) } }
    val moonLine = remember(state.startTime, state.moon) { state.moon.map { it.toVector(state.startTime) } }
    val sunPosition = state.sunPosition?.toVector(state.startTime)
    val moonPosition = state.moonPosition?.toVector(state.startTime)

    Column(modifier) {
        TrailSenseChartFrame(
            yLabels = listOf(
                stringResource(R.string.sky_axis_above),
                stringResource(R.string.horizon),
                stringResource(R.string.sky_axis_below)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(state) {
                        detectTapGestures { offset ->
                            val inset = TrailSenseChartInset.toPx()
                            val reach = MarkerReach.toPx()
                            val width = size.width.toFloat()
                            val height = size.height.toFloat()
                            val tappedBody = listOfNotNull(sunPosition, moonPosition).any {
                                val screen = toScreen(it, inset, width - inset, inset, height - inset)
                                (screen.x - offset.x).absoluteValue <= reach &&
                                    (screen.y - offset.y).absoluteValue <= reach
                            }
                            if (tappedBody) {
                                onClick(null)
                                return@detectTapGestures
                            }
                            val hour = (((offset.x - inset) / (width - 2 * inset)) * 24f).coerceIn(0f, 24f)
                            onClick(state.startTime.plus(Time.hours(hour.toDouble())).toZonedDateTime())
                        }
                    }
            ) {
                val inset = TrailSenseChartInset.toPx()
                val left = inset
                val right = size.width - inset
                val top = inset
                val bottom = size.height - inset

                if (state.showBands) {
                    listOf(
                        AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL,
                        AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL,
                        AstronomyService.SUN_MIN_ALTITUDE_CIVIL
                    ).forEach {
                        drawHorizontal(it, limitColor, left, right, top, bottom)
                    }
                }
                val horizonY = toScreen(Vector2(0f, 0f), left, right, top, bottom).y
                // The night under the horizon: a deeper band of sky, so the horizon reads as a real edge.
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(nightColor.copy(alpha = nightColor.alpha * 0.4f), nightColor),
                        startY = horizonY,
                        endY = size.height
                    ),
                    topLeft = Offset(0f, horizonY),
                    size = Size(size.width, size.height - horizonY)
                )
                // Daylight as a lake under the sun's curve while it is up, brightest where the sun is highest.
                if (sunLine.size >= 2) {
                    val screen = sunLine.map { toScreen(it, left, right, top, bottom) }
                    val peak = screen.minOf { it.y }
                    val lake = trailSenseSmoothPath(screen).apply {
                        lineTo(screen.last().x, horizonY)
                        lineTo(screen.first().x, horizonY)
                        close()
                    }
                    clipRect(left = left, top = 0f, right = right, bottom = horizonY) {
                        drawPath(
                            lake,
                            Brush.verticalGradient(
                                listOf(daylightColor, daylightColor.copy(alpha = daylightColor.alpha * 0.25f)),
                                startY = peak,
                                endY = horizonY
                            )
                        )
                    }
                }
                drawLine(
                    horizonColor,
                    Offset(left, horizonY),
                    Offset(right, horizonY),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )

                drawLineChart(
                    moonLine, moonColor, left, right, top, bottom, 2.dp.toPx(),
                    dash = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))
                )
                // The sun's path: full strength while it is up, quieter once it has set.
                clipRect(left = 0f, top = 0f, right = size.width, bottom = horizonY) {
                    drawLineChart(sunLine, sunColor, left, right, top, bottom, 3.5.dp.toPx(), smooth = true)
                }
                clipRect(left = 0f, top = horizonY, right = size.width, bottom = size.height) {
                    drawLineChart(sunLine, sunColor.copy(alpha = 0.45f), left, right, top, bottom, 3.dp.toPx(), smooth = true)
                }

                moonPosition?.let {
                    // A clean phase disc in a soft moonlight glow: the lit part pale, the shadow a quiet raised
                    // tone, a hairline rim so a thin crescent still reads as the whole moon.
                    val point = toScreen(it, left, right, top, bottom)
                    val imageSize = MoonSize.toPx()
                    val glow = imageSize / 2f + 12.dp.toPx()
                    drawCircle(
                        Brush.radialGradient(
                            listOf(MoonLit.copy(alpha = 0.22f), MoonLit.copy(alpha = 0f)),
                            center = point,
                            radius = glow
                        ),
                        glow,
                        point
                    )
                    drawCircle(ringColor, imageSize / 2f + 2.dp.toPx(), point)
                    val topLeft = Offset(point.x - imageSize / 2f, point.y - imageSize / 2f)
                    if (moonImage != null) {
                        drawImage(
                            moonImage,
                            dstOffset = IntOffset(topLeft.x.toInt(), topLeft.y.toInt()),
                            dstSize = IntSize(imageSize.toInt(), imageSize.toInt()),
                            colorFilter = moonFilter
                        )
                    } else {
                        translate(topLeft.x, topLeft.y) {
                            with(fallbackMoon) {
                                draw(Size(imageSize, imageSize), colorFilter = moonFilter)
                            }
                        }
                    }
                    drawCircle(moonOutline, imageSize / 2f, point, style = Stroke(1.dp.toPx()))
                }
                sunPosition?.let {
                    // The sun itself on its path: a warm disc in a soft halo (dimmer once it has set), on the
                    // chart's "now" line.
                    val point = toScreen(it, left, right, top, bottom)
                    val up = point.y <= horizonY
                    drawLine(
                        sunColor.copy(alpha = 0.5f),
                        Offset(point.x, 0f),
                        Offset(point.x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(
                        Brush.radialGradient(
                            listOf(SunWarm.copy(alpha = if (up) 0.45f else 0.18f), SunWarm.copy(alpha = 0f)),
                            center = point,
                            radius = 18.dp.toPx()
                        ),
                        18.dp.toPx(),
                        point
                    )
                    drawCircle(ringColor, 8.dp.toPx(), point)
                    drawCircle(if (up) SunWarm else SunWarm.copy(alpha = 0.6f), 6.dp.toPx(), point)
                }
            }
        }
        // The hours of the charted day under the plot; none is "now", so none is emphasised.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = TrailSenseChartGutter + TrailSenseChartInset - 4.dp, top = TrailSenseSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 6, 12, 18, 24).forEach { hour ->
                Text(
                    state.startTime.plus(Time.hours(hour.toDouble())).toZonedDateTime().hour.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun DrawScope.drawLineChart(
    points: List<Vector2>,
    color: Color,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float,
    strokeWidth: Float,
    dash: PathEffect? = null,
    smooth: Boolean = false
) {
    if (points.size < 2) return
    val screenPoints = points.map { toScreen(it, left, right, top, bottom) }
    val path = if (smooth) {
        trailSenseSmoothPath(screenPoints)
    } else {
        Path().apply {
            screenPoints.forEachIndexed { index, screen ->
                if (index == 0) moveTo(screen.x, screen.y) else lineTo(screen.x, screen.y)
            }
        }
    }
    drawPath(
        path,
        color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash)
    )
}

/** A dashed limit across the plot at altitude [value]. */
private fun DrawScope.drawHorizontal(
    value: Float,
    color: Color,
    left: Float,
    right: Float,
    top: Float,
    bottom: Float
) {
    val y = toScreen(Vector2(0f, value), left, right, top, bottom).y
    drawLine(
        color,
        Offset(left, y),
        Offset(right, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
    )
}

/** Hours since [start] and the altitude. */
private fun Reading<Float>.toVector(start: Instant): Vector2 {
    return Vector2(Duration.between(start, time).toMillis() / 3600000f, value)
}

private fun toScreen(point: Vector2, left: Float, right: Float, top: Float, bottom: Float): Offset {
    val x = left + (point.x.coerceIn(0f, 24f) / 24f) * (right - left)
    val y = bottom - ((point.y.coerceIn(MIN_Y, MAX_Y) - MIN_Y) / (MAX_Y - MIN_Y)) * (bottom - top)
    return Offset(x, y)
}

private const val MIN_Y = -100f
private const val MAX_Y = 100f

/** The moon disc's size on the chart; phase bitmaps for it are drawn at this size. */
internal val MoonSize = 24.dp

/** How close to the sun or moon a tap counts as a tap on it. */
private val MarkerReach = 24.dp

/** The sun's own warm colour on the chart, the same in light and dark. */
private val SunWarm = Color(0xFFFFC94D)

/** The deep blue of the night sky under the horizon. */
private val NightSky = Color(0xFF070B1F)

/** The moon's lit face: the real moon is pale whatever the theme. */
internal val MoonLit = Color(0xFFF4F4F0)

/**
 * Recolours a phase bitmap (white lit limb, near-black shadow) by its brightness: black becomes [shadow] and white
 * becomes [lit]. A 4x5 colour matrix with offsets in 0..255, usable by both Android and Compose colour filters.
 */
internal fun moonPhaseColorMatrix(
    shadow: Color,
    lit: Color
): FloatArray {
    fun row(s: Float, l: Float) = floatArrayOf(l - s, 0f, 0f, 0f, s * 255f)
    return row(shadow.red, lit.red) + row(shadow.green, lit.green) + row(shadow.blue, lit.blue) +
        floatArrayOf(0f, 0f, 0f, 1f, 0f)
}
