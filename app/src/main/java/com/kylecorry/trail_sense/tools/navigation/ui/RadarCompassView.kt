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
    }.getOrNull()?.takeIf { it.length == 4 && it.all(Char::isDigit) } ?: "6002"
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
    private val lobes = prefs.navigation.dialLobes
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

    /** Sky marks already drawn this frame. */
    private val skyBearings = mutableListOf<Float>()

    private var sunBackColor: Int = Color.YELLOW
    private var moonBackColor: Int = Color.DKGRAY
    private var moonInkColor: Int = Color.WHITE
    private var surfaceColor: Int = Color.WHITE
    private var hubColor: Int = Color.WHITE
    private var onPrimaryColor: Int = Color.WHITE
    private var outlineColor: Int = Color.GRAY
    private var onHubColor: Int = Color.WHITE
    private val hubPolygon = androidx.compose.material3.MaterialShapes.Cookie12Sided
    private val dialPolygon = androidx.compose.material3.MaterialShapes.Cookie12Sided
    private val hubMorph = androidx.graphics.shapes.Morph(
        androidx.compose.material3.MaterialShapes.Cookie12Sided,
        androidx.compose.material3.MaterialShapes.Circle
    )
    private var hubProgress = 0f
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
        outlineColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        onHubColor = MaterialTheme.colorScheme.onPrimaryContainer.toArgb()
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
        // The radar (nearby paths) draws only inside the dial's graduations, never past its edge.
        val clipRadius = if (DialVariant.bezel == 6) outerRadius * WAVY_RADIUS - dp(30f) else compassSize / 2f
        compassPath = Path().apply {
            addCircle(centerPixel.x, centerPixel.y, clipRadius, Path.Direction.CW)
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
        get() = min(canvasWidth, canvasHeight) / 2f - iconSize * 0.55f

    private fun drawRadar() {
        val nativeCanvas = canvas ?: return
        nativeCanvas.save()
        nativeCanvas.rotate(-azimuth, centerPixel.x, centerPixel.y)
        if (DialVariant.bezel == 6) {
            if (shouldDrawDial) drawWavyDial(nativeCanvas)
            skyBearings.clear()
            drawCompassLayers()
            nativeCanvas.restore()
            if (shouldDrawDial) drawWavyLubber(nativeCanvas)
            return
        }
        if (DialVariant.bezel == 5) {
            skyBearings.clear()
            drawCompassLayers()
            if (shouldDrawDial) {
                drawClassicDial(nativeCanvas)
                if (DialVariant.hub != 0) drawDetentHub(nativeCanvas)
            }
            drawRangeLabel(nativeCanvas)
            nativeCanvas.restore()
            if (shouldDrawDial) drawClassicCentre(nativeCanvas, crosshair = DialVariant.hub == 0)
            return
        }
        if (shouldDrawDial) drawBezel(nativeCanvas)
        skyBearings.clear()
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

    /**
     * The classic instrument (Apple Compass, a good watch bezel): fine graduations every 2°, longer every 10°, longest
     * every 30° with the degree beside it outside the ring; cardinal letters inside, north in the primary colour with
     * a small primary triangle. Everything turns with the dial; the numbers stay upright.
     */
    private fun drawClassicDial(nativeCanvas: AndroidCanvas) {
        val r = outerRadius * 0.86f
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.BUTT
        for (degrees in 0 until 360 step 5) {
            val major = degrees % 30 == 0
            val mid = degrees % 10 == 0
            paint.color = if (major) textColor else outlineColor
            paint.alpha = when { major -> 255; mid -> 160; else -> 100 }
            paint.strokeWidth = when { major -> dp(3f); mid -> dp(2f); else -> dp(1.5f) }
            val len = when { major -> dp(14f); mid -> dp(9f); else -> dp(6f) }
            val rad = Math.toRadians((degrees - 90).toDouble())
            val c = cos(rad).toFloat()
            val sn = sin(rad).toFloat()
            nativeCanvas.drawLine(centerPixel.x + c * r, centerPixel.y + sn * r, centerPixel.x + c * (r - len), centerPixel.y + sn * (r - len), paint)
        }
        // Degrees every 30° outside the ring, upright.
        paint.reset()
        paint.isAntiAlias = true
        paint.color = textColor
        paint.alpha = 150
        paint.textSize = sp(12f)
        paint.textAlign = Paint.Align.CENTER
        val numberRadius = r + dp(15f)
        for (degrees in 30 until 360 step 30) {
            if (degrees % 90 == 0) continue
            val rad = Math.toRadians((degrees - 90).toDouble())
            val x = centerPixel.x + cos(rad).toFloat() * numberRadius
            val y = centerPixel.y + sin(rad).toFloat() * numberRadius
            nativeCanvas.save()
            nativeCanvas.rotate(azimuth, x, y)
            nativeCanvas.drawText(degrees.toString(), x, y + sp(12f) * 0.36f, paint)
            nativeCanvas.restore()
        }
        // Cardinal letters inside the ring, upright; north in primary, with a triangle at the ring.
        val letterRadius = r - dp(16f) - cardinalSize * 0.9f
        listOf(0f to north, 90f to east, 180f to south, 270f to west).forEach { (degrees, text) ->
            val rad = Math.toRadians((degrees - 90).toDouble())
            val x = centerPixel.x + cos(rad).toFloat() * letterRadius
            val y = centerPixel.y + sin(rad).toFloat() * letterRadius
            nativeCanvas.save()
            nativeCanvas.rotate(azimuth, x, y)
            paint.reset()
            paint.isAntiAlias = true
            paint.color = if (degrees == 0f) primaryColor else textColor
            paint.textSize = cardinalSize * 1.15f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            nativeCanvas.drawText(text, x, y + cardinalSize * 0.42f, paint)
            nativeCanvas.restore()
        }
        paint.typeface = Typeface.DEFAULT
        // North's triangle takes the 0's place outside the ring, pointing in at the north tick.
        val tri = Path().apply {
            val base = centerPixel.y - r - dp(3f)
            moveTo(centerPixel.x, base + dp(10f))
            lineTo(centerPixel.x - dp(6f), base)
            lineTo(centerPixel.x + dp(6f), base)
            close()
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        nativeCanvas.drawPath(tri, paint)
    }

    /** Fixed to the phone: a bold lubber line across the top of the ring, and a thin crosshair at the centre. */
    /** The wavy dial's radius at [degrees] (dial frame): 12 soft lobes, peaks on the 30° marks, flattening with [flat]. */
    private fun wavyRadius(degrees: Float, flat: Float): Float {
        val r = outerRadius * WAVY_RADIUS
        val amplitude = WAVY_AMPLITUDE * (1f - flat)
        val wave = (1f - cos(Math.toRadians(degrees * lobes.toDouble())).toFloat()) / 2f
        return r * (1f - amplitude * wave)
    }

    /**
     * The dial is one shape: a 12-lobed cookie in the primary container that turns with you, with its graduations
     * engraved inside, following the wavy edge (the 30° ticks on the lobe peaks). Lined up with a cardinal, the waves
     * flatten into a circle and the ticks straighten into a ring. The letters sit outside, on the page.
     */
    private fun drawWavyDial(nativeCanvas: AndroidCanvas) {
        val toCardinal = kotlin.math.abs(deltaAngle(azimuth, (Math.round(azimuth / 90f) * 90f)))
        val target = (1f - (toCardinal / DETENT_DEGREES)).coerceIn(0f, 1f)
        hubProgress += (target - hubProgress) * 0.25f
        if (kotlin.math.abs(target - hubProgress) > 0.01f) requestRedraw()
        val flat = hubProgress

        val shape = android.graphics.Path()
        for (d in 0..360) {
            val rr = wavyRadius(d.toFloat(), flat)
            val rad = Math.toRadians((d - 90).toDouble())
            val x = centerPixel.x + cos(rad).toFloat() * rr
            val y = centerPixel.y + sin(rad).toFloat() * rr
            if (d == 0) shape.moveTo(x, y) else shape.lineTo(x, y)
        }
        shape.close()
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = hubColor
        nativeCanvas.drawPath(shape, paint)

        // Graduations inside, hugging the edge.
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val step = 360 / lobes
        for (degrees in 0 until 360 step 5) {
            val cardinal = degrees % 90 == 0
            val major = degrees % step == 0
            val mid = degrees % 15 == 0
            paint.color = onHubColor
            paint.alpha = when { cardinal -> 255; major -> 210; mid -> 130; else -> 80 }
            paint.strokeWidth = when { cardinal -> dp(4.5f); major -> dp(3f); mid -> dp(2f); else -> dp(1.5f) }
            val len = when { cardinal -> dp(18f); major -> dp(13f); mid -> dp(9f); else -> dp(6f) }
            val start = wavyRadius(degrees.toFloat(), flat) - dp(9f)
            val rad = Math.toRadians((degrees - 90).toDouble())
            val c = cos(rad).toFloat()
            val sn = sin(rad).toFloat()
            nativeCanvas.drawLine(centerPixel.x + c * start, centerPixel.y + sn * start, centerPixel.x + c * (start - len), centerPixel.y + sn * (start - len), paint)
        }

        // Letters outside the shape, upright; north in primary.
        val letterRadius = outerRadius * WAVY_RADIUS + dp(6f) + cardinalSize * 0.6f
        listOf(0f to north, 90f to east, 180f to south, 270f to west).forEach { (degrees, text) ->
            val rad = Math.toRadians((degrees - 90).toDouble())
            val x = centerPixel.x + cos(rad).toFloat() * letterRadius
            val y = centerPixel.y + sin(rad).toFloat() * letterRadius
            nativeCanvas.save()
            nativeCanvas.rotate(azimuth, x, y)
            paint.reset()
            paint.isAntiAlias = true
            paint.color = if (degrees == 0f) primaryColor else textColor
            paint.textSize = cardinalSize * 1.1f
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            nativeCanvas.drawText(text, x, y + cardinalSize * 0.4f, paint)
            nativeCanvas.restore()
        }
        paint.typeface = Typeface.DEFAULT
    }

    /** Fixed to the phone: a short rounded pill crossing the top of the dial, turning primary at the detent. */
    private fun drawWavyLubber(nativeCanvas: AndroidCanvas) {
        val top = centerPixel.y - wavyRadius(0f, 1f)
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = surfaceColor
        paint.strokeWidth = dp(9f)
        nativeCanvas.drawLine(centerPixel.x, top - dp(8f), centerPixel.x, top + dp(14f), paint)
        paint.color = androidx.core.graphics.ColorUtils.blendARGB(textColor, primaryColor, hubProgress)
        paint.strokeWidth = dp(5f)
        nativeCanvas.drawLine(centerPixel.x, top - dp(8f), centerPixel.x, top + dp(14f), paint)
    }

    /**
     * The hub on the classic dial: a 12-sided cookie in the primary container that turns with the dial, and eases into
     * a perfect circle as you line up with a cardinal direction, so the shape itself says "you're facing north".
     */
    private fun drawDetentHub(nativeCanvas: AndroidCanvas) {
        val toCardinal = kotlin.math.abs(deltaAngle(azimuth, (Math.round(azimuth / 90f) * 90f)))
        val target = (1f - (toCardinal / DETENT_DEGREES)).coerceIn(0f, 1f)
        hubProgress += (target - hubProgress) * 0.25f
        if (kotlin.math.abs(target - hubProgress) > 0.01f) requestRedraw()
        val side = outerRadius * 0.86f * 2f * 0.59f
        val path = android.graphics.Path()
        hubMorph.toPath(hubProgress, path)
        path.transform(android.graphics.Matrix().apply {
            setScale(side, side)
            postTranslate(centerPixel.x - side / 2f, centerPixel.y - side / 2f)
        })
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = hubColor
        nativeCanvas.drawPath(path, paint)
    }

    private fun drawClassicCentre(nativeCanvas: AndroidCanvas, crosshair: Boolean) {
        val r = outerRadius * 0.86f
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = surfaceColor
        paint.strokeWidth = dp(8f)
        nativeCanvas.drawLine(centerPixel.x, centerPixel.y - r - dp(4f), centerPixel.x, centerPixel.y - r + dp(20f), paint)
        // Lined up with a cardinal, the lubber takes the primary colour with the hub's detent.
        paint.color = androidx.core.graphics.ColorUtils.blendARGB(textColor, primaryColor, hubProgress)
        paint.strokeWidth = dp(4f)
        nativeCanvas.drawLine(centerPixel.x, centerPixel.y - r - dp(4f), centerPixel.x, centerPixel.y - r + dp(20f), paint)
        if (!crosshair) return
        paint.strokeWidth = dp(1.5f)
        paint.alpha = 120
        val arm = dp(18f)
        nativeCanvas.drawLine(centerPixel.x - arm, centerPixel.y, centerPixel.x + arm, centerPixel.y, paint)
        nativeCanvas.drawLine(centerPixel.x, centerPixel.y - arm, centerPixel.x, centerPixel.y + arm, paint)
    }

    /** The lubber mark: a small primary triangle above the dial, fixed to the phone. */
    private fun drawAzimuth(nativeCanvas: AndroidCanvas) {
        val bitmap = getBitmap(TrailIcons.ArrowTarget, iconSize)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.colorFilter = PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_IN)
        nativeCanvas.drawBitmap(bitmap, canvasWidth / 2f - iconSize / 2f, centerPixel.y - outerRadius - iconSize * 0.45f, paint)
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
            4 -> {
                // The dial is one big 12-lobed cookie: the lobes are the 30° graduations and turn with you.
                val path = dialPolygon.toPath().apply {
                    transform(android.graphics.Matrix().apply {
                        setScale(r * 2f, r * 2f)
                        postTranslate(centerPixel.x - r, centerPixel.y - r)
                    })
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.style = Paint.Style.FILL
                paint.color = secondaryColor
                nativeCanvas.drawPath(path, paint)
                return
            }
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
    /** 36 quiet ticks, one every 10°, with a bolder one at each cardinal under its letter. */
    private fun drawFineTicks(nativeCanvas: AndroidCanvas) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val outer = outerRadius * DOT_RING
        for (degrees in 0 until 360 step 10) {
            val cardinal = degrees % 90 == 0
            paint.color = textColor
            paint.alpha = if (cardinal) 230 else 110
            paint.strokeWidth = if (cardinal) dp(3f) else dp(1.5f)
            val len = if (cardinal) dp(14f) else dp(8f)
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
        val radius = outerRadius * if (DialVariant.bezel == 4) 0.68f else LETTER_RING
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

        // Two sky marks on nearly the same bearing (the sun and moon) never stack: the second steps inward.
        val isSky = reference.icon != TrailIcons.ArrowTarget && reference.tint == null
        val near = if (isSky) skyBearings.firstOrNull { kotlin.math.abs(deltaAngle(it, reference.bearing)) < SKY_GAP } else null
        // Spread along the ring, away from the mark already there, just enough that the two discs sit side by side.
        val shownBearing = near?.let { other ->
            val delta = deltaAngle(other, reference.bearing)
            other + (if (delta >= 0f) SKY_GAP else -SKY_GAP)
        } ?: reference.bearing
        if (isSky) skyBearings.add(shownBearing)
        nativeCanvas.save()
        nativeCanvas.rotate(shownBearing, centerPixel.x, centerPixel.y)
        nativeCanvas.translate(centerPixel.x, centerPixel.y - outerRadius * when (DialVariant.bezel) { 4 -> 0.86f; 5 -> 0.58f; 6 -> 0.62f; else -> MARK_RING })
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

        // Centred on the dial (not the canvas corner), inside the graduations.
        val turnRadius = if (DialVariant.bezel == 6) outerRadius * WAVY_RADIUS - dp(30f) else outerRadius * DOT_RING
        fun centred(radius: Float) = arcRect.set(centerPixel.x - radius, centerPixel.y - radius, centerPixel.x + radius, centerPixel.y + radius)
        centred(turnRadius)
        val sweep = deltaAngle(azimuth, bearing.bearing)
        nativeCanvas.drawArc(arcRect, azimuth - 90, sweep, true, paint)

        // The turn still to make, as a bold rounded arc on the dial's edge: the wedge is only its soft shadow.
        if (kotlin.math.abs(sweep) > 0.5f) {
            val stroke = dp(7f)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.strokeCap = Paint.Cap.ROUND
            paint.alpha = 255
            centred(turnRadius - stroke / 2f)
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
        const val DOT_RING = 0.86f

        /** The cardinal letters sit outside the ring (Apple Compass), leaving the inside to the reading and marks. */
        const val LETTER_RING = 0.965f

        /** The wavy dial's peak radius (fraction of the outer radius) and its wave depth (fraction of that). */
        const val WAVY_RADIUS = 0.87f
        const val WAVY_AMPLITUDE = 0.05f

        /** Within this many degrees of a cardinal, the hub starts rounding into a circle. */
        const val DETENT_DEGREES = 6f

        /** The smallest angle between two sky marks on the ring, so they sit side by side, never stacked. */
        const val SKY_GAP = 13f

        /** The sun, moon and beacons ride just inside the ticks. */
        const val MARK_RING = 0.72f

        /** The hub's width as a fraction of the dial's diameter. */
        const val HUB = 0.47f
    }
}
