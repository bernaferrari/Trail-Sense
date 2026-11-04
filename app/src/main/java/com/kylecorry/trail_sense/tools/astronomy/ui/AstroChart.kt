package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    private val sunImage = bitmapLoader.load(TrailIcons.Sun, imageSize)

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
        // Theme colours: the sun warm (tertiary), the moon cool (primary), night as a primary wash, light dashed guides.
        val scheme = androidx.compose.material3.MaterialTheme.colorScheme
        val textColor = scheme.onSurfaceVariant
        val lineColor = scheme.outlineVariant
        val sunColor = scheme.tertiary
        val moonColor = scheme.primary.copy(alpha = 0.7f)
        val nightColor = scheme.primaryContainer
        val horizonColor = scheme.outline
        // The horizon's name sits just under its line at the left, inside the night band, clear of the curves.
        val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scheme.onPrimaryContainer.copy(alpha = 0.75f).toArgb()
            textSize = Resources.sp(context, 11f)
            textAlign = Paint.Align.LEFT
            isFakeBoldText = true
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.copy(alpha = 0.7f).toArgb()
            textSize = Resources.sp(context, 10f)
            textAlign = Paint.Align.CENTER
        }
        Canvas(
            modifier = modifier
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
            val left = 12.dp.toPx()
            val right = size.width - 12.dp.toPx()
            val top = 12.dp.toPx()
            val bottom = size.height - 24.dp.toPx()

            if (showBands) {
                drawBand(AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL, MIN_Y, nightColor.copy(alpha = 0.25f), left, right, top, bottom)
                drawBand(AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL, MIN_Y, nightColor.copy(alpha = 0.3f), left, right, top, bottom)
                drawBand(AstronomyService.SUN_MIN_ALTITUDE_CIVIL, MIN_Y, nightColor.copy(alpha = 0.4f), left, right, top, bottom)
                drawBand(0f, MIN_Y, nightColor.copy(alpha = 0.6f), left, right, top, bottom)
                listOf(
                    AstronomyService.SUN_MIN_ALTITUDE_ASTRONOMICAL,
                    AstronomyService.SUN_MIN_ALTITUDE_NAUTICAL,
                    AstronomyService.SUN_MIN_ALTITUDE_CIVIL
                ).forEach {
                    drawHorizontal(it, lineColor, left, right, top, bottom, dashed = true)
                }
            } else {
                drawBand(0f, MIN_Y, nightColor.copy(alpha = 0.85f), left, right, top, bottom)
            }

            drawHorizontal(0f, horizonColor, left, right, top, bottom, strokeWidth = 1.5.dp.toPx(), dashed = true)
            drawContext.canvas.nativeCanvas.drawText(
                context.getString(R.string.horizon),
                left + 6.dp.toPx(),
                toScreen(Vector2(0f, 0f), left, right, top, bottom).y + 14.dp.toPx(),
                horizonPaint
            )

            drawArea(sunLine, sunPosition, sunColor.copy(alpha = 0.28f), left, right, top, bottom)
            drawArea(moonLine, moonPosition, moonColor.copy(alpha = 0.16f), left, right, top, bottom)
            drawLineChart(moonLine, moonColor, left, right, top, bottom, 2.dp.toPx())
            drawLineChart(sunLine, sunColor, left, right, top, bottom, 3.5.dp.toPx())

            sunPosition?.let {
                val point = toScreen(it, left, right, top, bottom)
                drawContext.canvas.nativeCanvas.drawBitmap(
                    sunImage,
                    point.x - imageSize / 2f,
                    point.y - imageSize / 2f,
                    null
                )
            }
            moonPosition?.let {
                val point = toScreen(it, left, right, top, bottom)
                val canvas = drawContext.canvas.nativeCanvas
                val image = moonBitmap ?: moonImage
                canvas.save()
                canvas.rotate(moonTilt, point.x, point.y)
                canvas.drawBitmap(
                    image,
                    point.x - image.width / 2f,
                    point.y - image.height / 2f,
                    null
                )
                canvas.restore()
            }

            repeat(7) { idx ->
                val hour = idx * 4f
                val x = left + (right - left) * (hour / 24f)
                drawContext.canvas.nativeCanvas.drawText(
                    startTime.plus(Time.hours(hour.toDouble())).toZonedDateTime().hour.toString(),
                    x,
                    size.height - 6.dp.toPx(),
                    textPaint
                )
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
        strokeWidth: Float
    ) {
        if (points.size < 2) return
        val path = Path()
        points.forEachIndexed { index, point ->
            val screen = toScreen(point, left, right, top, bottom)
            if (index == 0) {
                path.moveTo(screen.x, screen.y)
            } else {
                path.lineTo(screen.x, screen.y)
            }
        }
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }

    private fun DrawScope.drawArea(
        points: List<Vector2>,
        marker: Vector2?,
        color: Color,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) {
        val markerPoint = marker ?: return
        val areaPoints = points.filter { it.x <= markerPoint.x } + markerPoint
        if (areaPoints.size < 2) return
        val path = Path()
        val first = toScreen(areaPoints.first(), left, right, top, bottom)
        path.moveTo(first.x, bottom)
        areaPoints.forEach {
            val screen = toScreen(it, left, right, top, bottom)
            path.lineTo(screen.x, screen.y)
        }
        val last = toScreen(areaPoints.last(), left, right, top, bottom)
        path.lineTo(last.x, bottom)
        path.close()
        drawPath(path, color)
    }

    private fun DrawScope.drawBand(
        topValue: Float,
        bottomValue: Float,
        color: Color,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float
    ) {
        val topY = toScreen(Vector2(0f, topValue), left, right, top, bottom).y
        val bottomY = toScreen(Vector2(0f, bottomValue), left, right, top, bottom).y
        drawRect(
            color = color,
            topLeft = Offset(left, topY),
            size = androidx.compose.ui.geometry.Size(right - left, bottomY - topY)
        )
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

    private fun screenToHour(x: Float, width: Float): Float {
        val left = Resources.dp(context, 12f)
        val right = width - Resources.dp(context, 12f)
        return (((x - left) / (right - left)) * 24f).coerceIn(0f, 24f)
    }

    private fun isNear(offset: Offset, point: Vector2, width: Float, height: Float): Boolean {
        val left = Resources.dp(context, 12f)
        val right = width - Resources.dp(context, 12f)
        val top = Resources.dp(context, 12f)
        val bottom = height - Resources.dp(context, 24f)
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
