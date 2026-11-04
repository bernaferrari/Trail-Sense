package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartFrame
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartGutter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChartInset
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.drawTrailSenseNowDot
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSmoothPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.time.Time
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.ColorUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.navigation.ui.BitmapLoader
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.absoluteValue

class AstroChart(
    private val context: Context,
    private val onChartClicked: (time: ZonedDateTime?) -> Unit
) {

    private var renderVersion by mutableIntStateOf(0)
    private var startTime = Instant.now()
    private val bitmapLoader = BitmapLoader()
    private var previousMoonImage = TrailIcons.Moon
    private val imageSize = Resources.dp(context, 24f).toInt()

    private var showBands = false
    private var sunLine = emptyList<Vector2>()
    private var moonLine = emptyList<Vector2>()
    private var sunPosition: Vector2? = null
    private var moonPosition: Vector2? = null
    private var moonTilt = 0f
    private var moonBitmap: Bitmap? = null
    private var moonImage = bitmapLoader.load(TrailIcons.Moon, imageSize)

    fun setBands(show: Boolean) {
        if (show == showBands) return
        showBands = show
        renderVersion++
    }

    fun plot(sun: List<Reading<Float>>, moon: List<Reading<Float>>) {
        startTime = sun.firstOrNull()?.time ?: Instant.now()
        sunLine = getDataFromReadings(sun)
        moonLine = getDataFromReadings(moon)
        updateSunArea()
        updateMoonArea()
        renderVersion++
    }

    fun moveSun(position: Reading<Float>?) {
        sunPosition = position?.toVector()
        updateSunArea()
        renderVersion++
    }

    fun setMoonImage(icon: ImageVector) {
        moonBitmap = null
        moonImage = bitmapLoader.load(icon, imageSize)
        if (icon != previousMoonImage) {
            bitmapLoader.unload(previousMoonImage)
        }
        previousMoonImage = icon
        renderVersion++
    }

    fun setMoonImage(bitmap: Bitmap) {
        moonBitmap = bitmap
        moonTilt = 0f
        renderVersion++
    }

    fun moveMoon(position: Reading<Float>?, tilt: Float? = null) {
        moonPosition = position?.toVector()
        moonTilt = tilt ?: 0f
        updateMoonArea()
        renderVersion++
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        // The frame style: Above / Horizon / Below in the gutter, the sun as the one primary line, the moon a quiet
        // dotted line, the horizon (and twilight limits when asked for) as dashed limits, and the sun's place as the
        // "now" dot. Daylight (the sun's curve above the horizon) is one soft primaryContainer lake.
        val scheme = androidx.compose.material3.MaterialTheme.colorScheme
        val sunColor = scheme.primary
        val daylightColor = scheme.primaryContainer.copy(alpha = 0.55f)
        val moonColor = scheme.outline
        val limitColor = scheme.outlineVariant
        val horizonColor = scheme.outline
        val ringColor = scheme.background
        val moonOutline = scheme.outlineVariant
        val moonShadow = scheme.surfaceContainerHighest
        val moonPaint = androidx.compose.runtime.remember(moonShadow) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(moonPhaseColorMatrix(moonShadow, MoonLit))
            }
        }
        androidx.compose.foundation.layout.Column(modifier) {
            TrailSenseChartFrame(
                yLabels = listOf(
                    androidx.compose.ui.res.stringResource(R.string.sky_axis_above),
                    androidx.compose.ui.res.stringResource(R.string.horizon),
                    androidx.compose.ui.res.stringResource(R.string.sky_axis_below)
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(renderVersion) {
                            detectTapGestures { offset ->
                                val sun = sunPosition
                                val moon = moonPosition
                                if (sun != null && isNear(offset, sun, size.width.toFloat(), size.height.toFloat())) {
                                    onChartClicked(null)
                                    return@detectTapGestures
                                }
                                if (moon != null && isNear(offset, moon, size.width.toFloat(), size.height.toFloat())) {
                                    onChartClicked(null)
                                    return@detectTapGestures
                                }
                                val time = startTime.plus(Time.hours(screenToHour(offset.x, size.width.toFloat()).toDouble()))
                                    .toZonedDateTime()
                                onChartClicked(time)
                            }
                        }
                ) {
                    renderVersion
                    val inset = TrailSenseChartInset.toPx()
                    val left = inset
                    val right = size.width - inset
                    val top = inset
                    val bottom = size.height - inset

                    if (showBands) {
                        listOf(
                            AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL,
                            AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL,
                            AstronomyService.SUN_MIN_ALTITUDE_CIVIL
                        ).forEach {
                            drawHorizontal(it, limitColor, left, right, top, bottom, dashed = true)
                        }
                    }
                    // Daylight as a filled lake: the area under the sun's curve while it is above the horizon.
                    if (sunLine.size >= 2) {
                        val horizonY = toScreen(Vector2(0f, 0f), left, right, top, bottom).y
                        val screen = sunLine.map { toScreen(it, left, right, top, bottom) }
                        val lake = trailSenseSmoothPath(screen).apply {
                            lineTo(screen.last().x, horizonY)
                            lineTo(screen.first().x, horizonY)
                            close()
                        }
                        clipRect(left = left, top = 0f, right = right, bottom = horizonY) {
                            drawPath(lake, daylightColor)
                        }
                    }
                    drawHorizontal(0f, horizonColor, left, right, top, bottom, strokeWidth = 1.5.dp.toPx(), dashed = true)

                    drawLineChart(
                        moonLine, moonColor, left, right, top, bottom, 2.dp.toPx(),
                        dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))
                    )
                    drawLineChart(sunLine, sunColor, left, right, top, bottom, 3.5.dp.toPx(), smooth = true)

                    moonPosition?.let {
                        // A clean phase disc: the lit part pale, the shadow a quiet raised tone, a hairline rim so a
                        // thin crescent still reads as the whole moon. The sun's "now" dot stays the one primary accent.
                        val point = toScreen(it, left, right, top, bottom)
                        drawCircle(ringColor, imageSize / 2f + 3.dp.toPx(), point)
                        val canvas = drawContext.canvas.nativeCanvas
                        val image = moonBitmap ?: moonImage
                        canvas.save()
                        canvas.rotate(moonTilt, point.x, point.y)
                        canvas.drawBitmap(
                            image,
                            point.x - image.width / 2f,
                            point.y - image.height / 2f,
                            moonPaint
                        )
                        canvas.restore()
                        drawCircle(moonOutline, imageSize / 2f, point, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    }
                    sunPosition?.let {
                        drawTrailSenseNowDot(toScreen(it, left, right, top, bottom), sunColor, ringColor)
                    }
                }
            }
            // The hours of the charted day under the plot; none is "now", so none is emphasised.
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = TrailSenseChartGutter + TrailSenseChartInset - 4.dp, top = TrailSenseSpacing.sm),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                renderVersion
                listOf(0, 6, 12, 18, 24).forEach { hour ->
                    androidx.compose.material3.Text(
                        startTime.plus(Time.hours(hour.toDouble())).toZonedDateTime().hour.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
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
        dash: androidx.compose.ui.graphics.PathEffect? = null,
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
        drawPath(path, color, style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round, pathEffect = dash))

    }

    private fun DrawScope.drawHorizontal(
        value: Float,
        color: Color,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        strokeWidth: Float = 1.dp.toPx(),
        dashed: Boolean = false
    ) {
        val y = toScreen(Vector2(0f, value), left, right, top, bottom).y
        drawLine(
            color,
            Offset(left, y),
            Offset(right, y),
            strokeWidth = strokeWidth,
            pathEffect = if (dashed) {
                androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
            } else {
                null
            }
        )
    }

    private fun updateSunArea() {
        // Area fill is derived at draw time from the current marker.
    }

    private fun updateMoonArea() {
        // Area fill is derived at draw time from the current marker.
    }

    private fun Reading<Float>.toVector(): Vector2 {
        return Vector2(Duration.between(startTime, time).toMillis() / 3600000f, value)
    }

    private fun getDataFromReadings(readings: List<Reading<Float>>): List<Vector2> {
        return readings.map { it.toVector() }
    }

    private val chartInset = Resources.dp(context, 14f)

    private fun screenToHour(x: Float, width: Float): Float {
        val left = chartInset
        val right = width - chartInset
        return (((x - left) / (right - left)) * 24f).coerceIn(0f, 24f)
    }

    private fun isNear(offset: Offset, point: Vector2, width: Float, height: Float): Boolean {
        val left = chartInset
        val right = width - chartInset
        val top = chartInset
        val bottom = height - chartInset
        val screen = toScreen(point, left, right, top, bottom)
        return (screen.x - offset.x).absoluteValue <= Resources.dp(context, 24f) &&
            (screen.y - offset.y).absoluteValue <= Resources.dp(context, 24f)
    }

    private fun toScreen(point: Vector2, left: Float, right: Float, top: Float, bottom: Float): Offset {
        val x = left + (point.x.coerceIn(0f, 24f) / 24f) * (right - left)
        val y = bottom - ((point.y.coerceIn(MIN_Y, MAX_Y) - MIN_Y) / (MAX_Y - MIN_Y)) * (bottom - top)
        return Offset(x, y)
    }

    companion object {
        private const val MIN_Y = -100f
        private const val MAX_Y = 100f
    }
}

/** The moon's lit face: the real moon is pale whatever the theme. */
internal val MoonLit = androidx.compose.ui.graphics.Color(0xFFF4F4F0)

/**
 * Recolours a phase bitmap (white lit limb, near-black shadow) by its brightness: black becomes [shadow] and white
 * becomes [lit]. A 4x5 colour matrix with offsets in 0..255, usable by both Android and Compose colour filters.
 */
internal fun moonPhaseColorMatrix(
    shadow: androidx.compose.ui.graphics.Color,
    lit: androidx.compose.ui.graphics.Color
): FloatArray {
    fun row(s: Float, l: Float) = floatArrayOf(l - s, 0f, 0f, 0f, s * 255f)
    return row(shadow.red, lit.red) + row(shadow.green, lit.green) + row(shadow.blue, lit.blue) +
        floatArrayOf(0f, 0f, 0f, 1f, 0f)
}
