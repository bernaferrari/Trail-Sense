package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.tools.map.ui.MapView
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class RadarCompassView(
    context: Context
) : BaseCompassView(context) {

    @ColorInt
    private var primaryColor: Int = Color.WHITE

    @ColorInt
    private var secondaryColor: Int = Color.WHITE

    @ColorInt
    private var textColor: Int = Color.WHITE

    /** The turn still to make is drawn in the theme's warm tertiary, so it belongs to the app's palette. */
    @ColorInt
    private var turnColor: Int = Color.WHITE

    private val formatService by lazy { FormatService.getInstance(context) }
    private val drawDialBezel = prefs.navigation.drawDialBezel
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val arcRect = RectF()
    private var mapView: MapView? = null
    private var canvas: AndroidCanvas? = null
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var centerPixel = PixelCoordinate(0f, 0f)
    private var compassPath = Path()
    private var iconSize = Resources.dp(context, 24f).toInt()
    private var compassSize = 0
    private var distanceSize = Resources.sp(context, 10f)
    private var cardinalSize = Resources.sp(context, 20f)
    private var dialTickRadius = 0f
    private var maxDistanceBaseUnits: Distance = Distance.meters(0f)
    private var maxDistanceMeters: Distance = Distance.meters(0f)
    private var north = context.getString(R.string.direction_north)
    private var south = context.getString(R.string.direction_south)
    private var east = context.getString(R.string.direction_east)
    private var west = context.getString(R.string.direction_west)
    private var distanceText: String? = null

    var shouldDrawDial: Boolean = true
        set(value) {
            field = value
            requestRedraw()
        }

    var shouldDrawAzimuthIndicator: Boolean = true
        set(value) {
            field = value
            requestRedraw()
        }

    private var sunBackColor: Int = Color.YELLOW
    private var moonBackColor: Int = Color.DKGRAY
    private var moonInkColor: Int = Color.WHITE
    private var surfaceColor: Int = Color.WHITE

    @Composable
    override fun Content(modifier: Modifier) {
        primaryColor = MaterialTheme.colorScheme.primary.toArgb()
        secondaryColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
        textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        turnColor = MaterialTheme.colorScheme.tertiary.toArgb()
        sunBackColor = MaterialTheme.colorScheme.tertiaryContainer.toArgb()
        moonBackColor = MaterialTheme.colorScheme.secondaryContainer.toArgb()
        moonInkColor = MaterialTheme.colorScheme.onSecondaryContainer.toArgb()
        surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged {
                    updateContentSize(it.width, it.height)
                }
        ) {
            renderVersion
            if (!isVisible) {
                return@Canvas
            }
            canvas = drawContext.canvas.nativeCanvas
            canvasWidth = size.width
            canvasHeight = size.height
            updateGeometry()
            drawRadar()
            canvas = null
        }
    }

    fun setRadiusDistance(distance: Distance) {
        maxDistanceMeters = distance.meters()
        maxDistanceBaseUnits = maxDistanceMeters.convertTo(prefs.baseDistanceUnits)
        distanceText = null
        requestRedraw()
    }

    fun bindMapView(view: MapView) {
        mapView = view
        updateGeometry()
        requestRedraw()
    }

    private fun updateGeometry() {
        if (canvasWidth <= 0f || canvasHeight <= 0f) {
            return
        }
        iconSize = dp(24f).toInt()
        compassSize = min(canvasHeight, canvasWidth).toInt() - 2 * iconSize - 2 * dp(2f).toInt()
        centerPixel = PixelCoordinate(canvasWidth / 2f, canvasHeight / 2f)
        compassPath = Path().apply {
            addCircle(centerPixel.x, centerPixel.y, compassSize / 2f, Path.Direction.CW)
        }
        distanceSize = sp(10f)
        cardinalSize = sp(18f)
        dialTickRadius = if (drawDialBezel) {
            compassSize / 2f + iconSize / 2f + dp(2f)
        } else {
            (compassSize / 2f) * 0.9f
        }
        mapView?.clipPath = compassPath
    }

    private fun drawRadar() {
        val nativeCanvas = canvas ?: return
        nativeCanvas.save()
        nativeCanvas.rotate(-azimuth, centerPixel.x, centerPixel.y)
        drawDial(nativeCanvas, drawTicks = false, drawBackground = true)
        if (shouldDrawAzimuthIndicator) {
            nativeCanvas.save()
            nativeCanvas.rotate(azimuth, centerPixel.x, centerPixel.y)
            drawAzimuth(nativeCanvas)
            nativeCanvas.restore()
        }
        drawCompassLayers()
        drawCompass(nativeCanvas)
        nativeCanvas.restore()
    }

    private fun drawAzimuth(nativeCanvas: AndroidCanvas) {
        val bitmap = getBitmap(TrailIcons.ArrowTarget, iconSize)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.colorFilter = PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
        nativeCanvas.drawBitmap(bitmap, canvasWidth / 2f - iconSize / 2f, 0f, paint)
        paint.colorFilter = null
    }

    private fun drawCompass(nativeCanvas: AndroidCanvas) {
        drawDial(nativeCanvas, drawTicks = shouldDrawDial, drawBackground = false)

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.alpha = 36
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        nativeCanvas.save()
        nativeCanvas.rotate(azimuth, centerPixel.x, centerPixel.y)
        if (shouldDrawDial && shouldDrawAzimuthIndicator) {
            nativeCanvas.drawLine(
                centerPixel.x,
                centerPixel.y,
                centerPixel.x,
                iconSize + dp(2f),
                paint
            )
        }
        val compassRadius = compassSize / 2f
        nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, compassRadius, paint)
        nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, 3 * compassRadius / 4f, paint)
        nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, compassRadius / 2f, paint)

        if (distanceText == null) {
            val distance = maxDistanceBaseUnits.toRelativeDistance()
            distanceText = formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )
        }

        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.alpha = 200
        paint.textSize = distanceSize
        paint.textAlign = Paint.Align.LEFT
        distanceText?.takeIf { maxDistanceMeters.value > 0f }?.let {
            nativeCanvas.drawText(
                it,
                (canvasWidth - compassSize) / 2f + 16,
                canvasHeight - (canvasHeight - compassSize) / 2f + 16,
                paint
            )
        }
        nativeCanvas.restore()

        if (shouldDrawDial) {
            drawCardinalDirections(nativeCanvas)
        }
    }

    private fun drawCardinalDirections(nativeCanvas: AndroidCanvas) {
        drawDirection(nativeCanvas, 0f, north, primaryColor)
        drawDirection(nativeCanvas, 90f, east, textColor)
        drawDirection(nativeCanvas, 180f, south, textColor)
        drawDirection(nativeCanvas, 270f, west, textColor)
    }

    private fun drawDirection(
        nativeCanvas: AndroidCanvas,
        degrees: Float,
        text: String,
        @ColorInt color: Int
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = color
        paint.textSize = cardinalSize
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        nativeCanvas.save()
        nativeCanvas.rotate(degrees, centerPixel.x, centerPixel.y)
        // Inside the ring: clear of the longest tick, vertically centered on its radius.
        val letterRadius = dialTickRadius - dp(10f) - cardinalSize * 0.5f
        nativeCanvas.drawText(text, centerPixel.x, centerPixel.y - letterRadius + cardinalSize * 0.36f, paint)
        nativeCanvas.restore()
        paint.typeface = Typeface.DEFAULT
    }

    private fun drawDial(
        nativeCanvas: AndroidCanvas,
        drawTicks: Boolean = true,
        drawBackground: Boolean = true
    ) {
        val backgroundRadius = compassSize / 2f
        if (drawBackground) {
            paint.reset()
            paint.isAntiAlias = true
            paint.style = Paint.Style.FILL
            paint.color = Color.TRANSPARENT
            nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, backgroundRadius, paint)
            if (drawDialBezel) {
                paint.color = secondaryColor
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (min(canvasHeight, canvasWidth) - compassSize) / 2f
                nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, backgroundRadius + paint.strokeWidth / 2f, paint)
            }
        }

        if (!drawTicks) {
            return
        }

        // A tonal bezel band under the graduations and letters, the family's tone language on an instrument. The
        // middle stays clear so a map under the radar still shows through.
        if (!drawDialBezel) {
            paint.reset()
            paint.isAntiAlias = true
            paint.style = Paint.Style.STROKE
            paint.color = secondaryColor
            val outer = backgroundRadius
            val inner = dialTickRadius - dp(10f) - cardinalSize - dp(6f)
            paint.strokeWidth = outer - inner
            nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, (outer + inner) / 2f, paint)
        }

        // Fine graduations every 5°, stronger every 15° and 45°, and north in the accent color: a dial that reads
        // like an instrument without crowding the letters, which sit inside the ring.
        drawTicks(nativeCanvas, 5, dp(4f), dp(1f), textColor.withAlpha(90))
        drawTicks(nativeCanvas, 15, dp(8f), dp(1.5f), textColor.withAlpha(170))
        drawTicks(nativeCanvas, 45, dp(12f), dp(2.5f), textColor)
        paint.color = primaryColor
        paint.strokeWidth = dp(3f)
        drawTick(nativeCanvas, 0f, dp(14f), paint)
    }

    private fun drawTicks(
        nativeCanvas: AndroidCanvas,
        interval: Int,
        length: Float,
        strokeWidth: Float,
        @ColorInt color: Int
    ) {
        paint.reset()
        paint.isAntiAlias = true
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.SQUARE
        paint.strokeWidth = strokeWidth + dp(1f)
        for (degrees in 0 until 360 step interval) {
            drawTick(nativeCanvas, degrees.toFloat(), length, paint)
        }

        paint.color = color
        paint.strokeWidth = strokeWidth
        for (degrees in 0 until 360 step interval) {
            drawTick(nativeCanvas, degrees.toFloat(), length, paint)
        }
        paint.strokeCap = Paint.Cap.ROUND
    }

    private fun drawTick(
        nativeCanvas: AndroidCanvas,
        degrees: Float,
        length: Float,
        paint: Paint
    ) {
        val radians = Math.toRadians((degrees - 90).toDouble())
        val startRadius = dialTickRadius - length / 2f
        val endRadius = dialTickRadius + length / 2f
        val startX = centerPixel.x + cos(radians).toFloat() * startRadius
        val startY = centerPixel.y + sin(radians).toFloat() * startRadius
        val endX = centerPixel.x + cos(radians).toFloat() * endRadius
        val endY = centerPixel.y + sin(radians).toFloat() * endRadius
        nativeCanvas.drawLine(startX, startY, endX, endY, paint)
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        val nativeCanvas = canvas ?: return
        val sizeDp = size?.let { dp(it.toFloat()).toInt() } ?: iconSize
        if (reference.opacity == 0f) {
            return
        }

        val bitmap = getBitmap(reference.icon, sizeDp)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.alpha = (255 * reference.opacity).toInt()
        // The moon's art renders dark on the dial; it takes the disc's on-colour instead, a quiet badge.
        val tint = reference.tint ?: moonInkColor.takeIf { reference.icon != TrailIcons.Sun }
        paint.colorFilter = tint?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }

        nativeCanvas.save()
        nativeCanvas.rotate(reference.bearing, centerPixel.x, centerPixel.y)
        nativeCanvas.translate(centerPixel.x, iconSize / 2f + dp(1f))
        // The sun and moon ride the rim on a small disc of their own (a warm tertiary disc for the sun, a secondary one for the moon),
        // so the moon's pale art never reads as a hole in the dial.
        // A beacon is a waypoint dot in its own colour, ringed in the dial's surface (Apple Compass-style): the wedge
        // and arc already point at it, so it doesn't need an arrow of its own. Its icon, if any, rides on top.
        if (reference.icon == TrailIcons.ArrowTarget) {
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (255 * reference.opacity).toInt() }
            dot.color = surfaceColor
            dot.alpha = (255 * reference.opacity).toInt()
            nativeCanvas.drawCircle(0f, 0f, sizeDp * 0.46f, dot)
            dot.color = reference.tint ?: primaryColor
            dot.alpha = (255 * reference.opacity).toInt()
            nativeCanvas.drawCircle(0f, 0f, sizeDp * 0.36f, dot)
            nativeCanvas.restore()
            paint.alpha = 255
            paint.colorFilter = null
            return
        }
        // Tinted references (beacons) keep their own mark; only the untinted sky art gets a disc.
        if (reference.tint == null) {
            val back = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (reference.icon == TrailIcons.Sun) sunBackColor else moonBackColor
                alpha = (255 * reference.opacity).toInt()
            }
            nativeCanvas.drawCircle(0f, 0f, sizeDp / 2f + dp(3f), back)
        }
        nativeCanvas.rotate(reference.rotation)
        nativeCanvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, paint)
        nativeCanvas.restore()
        paint.alpha = 255
        paint.colorFilter = null
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        val nativeCanvas = canvas ?: return
        paint.reset()
        paint.isAntiAlias = true
        paint.color = turnColor
        paint.style = Paint.Style.FILL
        paint.alpha = if (stopAt != null) 18 else 40

        val inset = iconSize.toFloat() + dp(2f)
        arcRect.set(inset, inset, inset + compassSize, inset + compassSize)
        val sweep = deltaAngle(azimuth, bearing.bearing)
        nativeCanvas.drawArc(arcRect, azimuth - 90, sweep, true, paint)

        // The turn still to make, as a bold rounded arc on the dial's edge: the wedge is only its soft shadow.
        if (kotlin.math.abs(sweep) > 0.5f) {
            val stroke = dp(7f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.strokeCap = Paint.Cap.ROUND
            paint.alpha = 255
            val ring = compassSize * 0.155f + stroke / 2f + dp(4f)
            arcRect.inset(ring, ring)
            // The rounded caps end exactly on the wedge's edges instead of overhanging them.
            val capDegrees = Math.toDegrees((stroke / 2f / (arcRect.width() / 2f)).toDouble()).toFloat()
            if (kotlin.math.abs(sweep) > capDegrees * 2) {
                val direction = kotlin.math.sign(sweep)
                nativeCanvas.drawArc(
                    arcRect,
                    azimuth - 90 + direction * capDegrees,
                    sweep - direction * capDegrees * 2,
                    false,
                    paint
                )
            }
            paint.style = Paint.Style.FILL
            paint.strokeCap = Paint.Cap.BUTT
            arcRect.set(inset, inset, inset + compassSize, inset + compassSize)
        }

        stopAt?.let {
            val pixel = mapView?.toPixel(it) ?: return@let
            val size = min(compassSize.toFloat(), pixel.distanceTo(centerPixel) * 2)
            paint.alpha = 40
            arcRect.set(
                centerPixel.x - size / 2f,
                centerPixel.y - size / 2f,
                centerPixel.x + size / 2f,
                centerPixel.y + size / 2f
            )
            nativeCanvas.drawArc(
                arcRect,
                azimuth - 90,
                deltaAngle(azimuth, bearing.bearing),
                true,
                paint
            )
        }
        paint.alpha = 255
    }
}
