@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.star
import androidx.graphics.shapes.RoundedPolygon
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

/**
 * Dial design experiments, switchable on a debug build without a rebuild:
 * `adb shell setprop debug.ts.dial BGSH` then relaunch. B bezel (0 none, 1 ring, 2 disc, 3 scallop),
 * G graduations (0 none, 1 eight dots, 2 dots every 15°, 3 fine ticks), S sky marks (0 hidden, 1 shown),
 * H hub (0 none, 1 circle, 2 cookie 12, 3 cookie 9, 4 sunny).
 */
object DialVariant {
    val code: String = runCatching {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java)
            .invoke(null, "debug.ts.dial") as String
    }.getOrNull()?.takeIf { it.length == 4 && it.all(Char::isDigit) } ?: "1312"
    val bezel get() = code[0].digitToInt()
    val graduations get() = code[1].digitToInt()
    val sky get() = code[2] == '1'
    val hub get() = code[3].digitToInt()
}

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
    private var hubColor: Int = Color.WHITE
    private var onPrimaryColor: Int = Color.WHITE
    private val hubPolygon = androidx.compose.material3.MaterialShapes.Cookie12Sided
    private val bezelPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 18,
        radius = 1f,
        innerRadius = 0.96f,
        rounding = androidx.graphics.shapes.CornerRounding(0.09f),
        innerRounding = androidx.graphics.shapes.CornerRounding(0.09f)
    )

    @Composable
    override fun Content(modifier: Modifier) {
        primaryColor = MaterialTheme.colorScheme.primary.toArgb()
        secondaryColor = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
        textColor = MaterialTheme.colorScheme.onSurface.toArgb()
        turnColor = MaterialTheme.colorScheme.tertiary.toArgb()
        sunBackColor = MaterialTheme.colorScheme.tertiaryContainer.toArgb()
        moonBackColor = MaterialTheme.colorScheme.surfaceContainerHighest.toArgb()
        moonInkColor = MaterialTheme.colorScheme.outline.toArgb()
        surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
        hubColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
        onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
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

    /** The dial's outer radius: the whole canvas, so sky marks and beacons ride on the bezel's outer edge. */
    private val outerRadius: Float
        get() = min(canvasWidth, canvasHeight) / 2f - iconSize * 0.8f

    private fun drawRadar() {
        val nativeCanvas = canvas ?: return
        nativeCanvas.save()
        nativeCanvas.rotate(-azimuth, centerPixel.x, centerPixel.y)
        if (shouldDrawDial) drawBezel(nativeCanvas)
        drawCompassLayers()
        if (shouldDrawDial) {
            when (DialVariant.graduations) {
                1, 2 -> drawDots(nativeCanvas, if (DialVariant.graduations == 1) 45 else 15)
                3 -> drawFineTicks(nativeCanvas)
            }
            if (DialVariant.hub != 0) drawHub(nativeCanvas)
            drawCardinalDirections(nativeCanvas)
        }
        drawRangeLabel(nativeCanvas)
        nativeCanvas.restore()
        if (shouldDrawAzimuthIndicator) drawAzimuth(nativeCanvas)
    }

    /** The lubber mark: a small primary triangle above the dial, fixed to the phone. */
    private fun drawAzimuth(nativeCanvas: AndroidCanvas) {
        val bitmap = getBitmap(TrailIcons.ArrowTarget, iconSize)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.colorFilter = PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
        nativeCanvas.drawBitmap(bitmap, canvasWidth / 2f - iconSize / 2f, centerPixel.y - outerRadius - iconSize * 0.75f, paint)
        paint.colorFilter = null
    }

    /**
     * The bezel: a softly scalloped disc (M3 Expressive's wavy edge) in the container tone, fixed to the dial so its
     * waves turn as you turn. The map, when a radar layer is on, shows through inside it.
     */
    private fun drawBezel(nativeCanvas: AndroidCanvas) {
        val r = outerRadius
        when (DialVariant.bezel) {
            0 -> return
            1 -> {
                paint.reset()
                paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = dp(2f)
                paint.color = secondaryColor
                nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, r * DOT_RING, paint)
                return
            }
            2 -> {
                paint.reset()
                paint.isAntiAlias = true
                paint.style = Paint.Style.FILL
                paint.color = secondaryColor
                nativeCanvas.drawCircle(centerPixel.x, centerPixel.y, r * DOT_RING + dp(4f), paint)
                return
            }
        }
        val path = bezelPolygon.toPath().apply {
            transform(android.graphics.Matrix().apply {
                setScale(r, r)
                postTranslate(centerPixel.x, centerPixel.y)
            })
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = secondaryColor
        nativeCanvas.drawPath(path, paint)
    }

    /** A ring of dots every 15°, larger every 45°: graduations that read at a glance without 72 hairlines. */
    /** Fine ticks every 5°, longer every 45°, the classic instrument graduation. */
    private fun drawFineTicks(nativeCanvas: AndroidCanvas) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val outer = outerRadius * DOT_RING
        for (degrees in 0 until 360 step 5) {
            if (degrees % 90 == 0) continue
            val major = degrees % 45 == 0
            paint.color = textColor
            paint.alpha = if (major) 220 else 80
            paint.strokeWidth = if (major) dp(2.5f) else dp(1.2f)
            val len = if (major) dp(12f) else dp(7f)
            val rad = Math.toRadians((degrees - 90).toDouble())
            val c = cos(rad).toFloat()
            val sn = sin(rad).toFloat()
            nativeCanvas.drawLine(centerPixel.x + c * outer, centerPixel.y + sn * outer, centerPixel.x + c * (outer - len), centerPixel.y + sn * (outer - len), paint)
        }
    }

    private fun drawDots(nativeCanvas: AndroidCanvas, step: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        val radius = outerRadius * DOT_RING
        for (degrees in 0 until 360 step step) {
            if (degrees % 90 == 0) continue
            val major = degrees % 45 == 0
            paint.color = textColor
            paint.alpha = if (major) 200 else 90
            val rad = Math.toRadians((degrees - 90).toDouble())
            nativeCanvas.drawCircle(
                centerPixel.x + cos(rad).toFloat() * radius,
                centerPixel.y + sin(rad).toFloat() * radius,
                if (major) dp(3.5f) else dp(2.5f),
                paint
            )
        }
    }

    /**
     * The hub: a 12-sided cookie in the primary container at the dial's centre, holding the heading. It is fixed to
     * the dial, so it turns as you turn; the shape itself tells you the phone is moving.
     */
    private fun drawHub(nativeCanvas: AndroidCanvas) {
        val side = outerRadius * 2f * HUB
        val polygon = when (DialVariant.hub) {
            1 -> androidx.compose.material3.MaterialShapes.Circle
            3 -> androidx.compose.material3.MaterialShapes.Cookie9Sided
            4 -> androidx.compose.material3.MaterialShapes.Sunny
            else -> hubPolygon
        }
        val path = polygon.toPath().apply {
            transform(android.graphics.Matrix().apply {
                setScale(side, side)
                postTranslate(centerPixel.x - side / 2f, centerPixel.y - side / 2f)
            })
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = hubColor
        nativeCanvas.drawPath(path, paint)
    }

    private fun drawRangeLabel(nativeCanvas: AndroidCanvas) {
        if (maxDistanceMeters.value <= 0f) return
        if (distanceText == null) {
            val distance = maxDistanceBaseUnits.toRelativeDistance()
            distanceText = formatService.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
        }
        nativeCanvas.save()
        nativeCanvas.rotate(azimuth, centerPixel.x, centerPixel.y)
        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.alpha = 160
        paint.textSize = distanceSize
        paint.textAlign = Paint.Align.CENTER
        distanceText?.let {
            nativeCanvas.drawText(it, centerPixel.x, centerPixel.y + outerRadius * 0.9f, paint)
        }
        nativeCanvas.restore()
    }

    /** North is a filled primary disc with its letter; the other three are bold letters, upright as you turn. */
    private fun drawCardinalDirections(nativeCanvas: AndroidCanvas) {
        drawDirection(nativeCanvas, 0f, north, isNorth = true)
        drawDirection(nativeCanvas, 90f, east, isNorth = false)
        drawDirection(nativeCanvas, 180f, south, isNorth = false)
        drawDirection(nativeCanvas, 270f, west, isNorth = false)
    }

    private fun drawDirection(nativeCanvas: AndroidCanvas, degrees: Float, text: String, isNorth: Boolean) {
        val radius = outerRadius * LETTER_RING
        val rad = Math.toRadians((degrees - 90).toDouble())
        val x = centerPixel.x + cos(rad).toFloat() * radius
        val y = centerPixel.y + sin(rad).toFloat() * radius
        nativeCanvas.save()
        // Keep the letters upright on screen: undo the dial's rotation around the letter itself.
        nativeCanvas.rotate(azimuth, x, y)
        paint.reset()
        paint.isAntiAlias = true
        if (isNorth) {
            paint.style = Paint.Style.FILL
            paint.color = primaryColor
            nativeCanvas.drawCircle(x, y, cardinalSize * 0.9f, paint)
        }
        paint.color = if (isNorth) onPrimaryColor else textColor
        paint.textSize = cardinalSize
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        nativeCanvas.drawText(text, x, y + cardinalSize * 0.36f, paint)
        nativeCanvas.restore()
        paint.typeface = Typeface.DEFAULT
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        val nativeCanvas = canvas ?: return
        val sizeDp = size?.let { dp(it.toFloat()).toInt() } ?: iconSize
        if (reference.opacity == 0f) {
            return
        }
        if (!DialVariant.sky && reference.icon != TrailIcons.ArrowTarget && reference.tint == null) {
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
        nativeCanvas.translate(centerPixel.x, centerPixel.y - outerRadius * MARK_RING)
        // The sun and moon ride the rim on a small disc of their own (a warm tertiary disc for the sun, a secondary one for the moon),
        // so the moon's pale art never reads as a hole in the dial.
        // A beacon is a waypoint dot in its own colour, ringed in the dial's surface (Apple Compass-style): the wedge
        // and arc already point at it, so it doesn't need an arrow of its own. Its icon, if any, rides on top.
        if (reference.icon == TrailIcons.ArrowTarget) {
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (255 * reference.opacity).toInt() }
            if (reference.shape == null) {
                dot.color = surfaceColor
                dot.alpha = (255 * reference.opacity).toInt()
                nativeCanvas.drawCircle(0f, 0f, sizeDp * 0.46f, dot)
            }
            dot.color = reference.tint ?: primaryColor
            dot.alpha = (255 * reference.opacity).toInt()
            val polygon = reference.shape
            if (polygon != null) {
                // The beacon's own shape, ringed in the dial's surface: the same mark as its list row and card.
                fun shapePath(side: Float): android.graphics.Path = polygon.toPath().apply {
                    transform(android.graphics.Matrix().apply {
                        setScale(side, side)
                        postTranslate(-side / 2f, -side / 2f)
                    })
                }
                val ring = dot.color
                dot.color = surfaceColor
                dot.alpha = (255 * reference.opacity).toInt()
                nativeCanvas.drawPath(shapePath(sizeDp * 1.0f), dot)
                dot.color = ring
                dot.alpha = (255 * reference.opacity).toInt()
                nativeCanvas.drawPath(shapePath(sizeDp * 0.8f), dot)
            } else {
                nativeCanvas.drawCircle(0f, 0f, sizeDp * 0.36f, dot)
            }
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
            // A ring in the bezel's tone, so the sun and moon stack like avatars when they share a bearing.
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = secondaryColor; alpha = back.alpha }
            nativeCanvas.drawCircle(0f, 0f, sizeDp / 2f + dp(5f), ring)
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
            val arcRadius = outerRadius * DOT_RING
            arcRect.set(centerPixel.x - arcRadius, centerPixel.y - arcRadius, centerPixel.x + arcRadius, centerPixel.y + arcRadius)
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

    private companion object {
        /** Where the dots, letters and turn arc sit, as a fraction of the outer radius. */
        const val DOT_RING = 0.84f

        /** The cardinal letters sit outside the ring (Apple Compass), leaving the inside to the reading and marks. */
        const val LETTER_RING = 0.95f

        /** The sun, moon and beacons ride just inside the ticks. */
        const val MARK_RING = 0.68f

        /** The hub's width as a fraction of the dial's diameter. */
        const val HUB = 0.50f
    }
}
