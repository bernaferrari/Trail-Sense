package com.kylecorry.trail_sense.tools.map.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalHapticFeedback
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.optimization.Optimization
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geography.projections.MercatorProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min


class MapView(private val context: Context) : IMapView {
    override var isWidget: Boolean = false
    var isInteractive = true
    var isPanEnabled = true
    var isZoomEnabled = true
    var isFlingEnabled = true
    var useDensityPixelsForZoom = true
    var id: Int = 0
    var keepScreenOn: Boolean = false
    var isInvisible: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var backgroundTintList: ColorStateList? = null
        set(value) {
            field = value
            backgroundColorOverride = value?.defaultColor
            invalidate()
        }
    override val isHighDetailMode: Boolean
        get() = !useDensityPixelsForZoom
    var clipPath: Path? = null
    var backgroundColorOverride: Int? = null
    private val density = context.resources.displayMetrics.density
    // A fling glides the map on after the finger lifts, slowing exponentially (pixels per second, map frame).
    private var flingVelocity = Offset.Zero
    private var lastFlingFrameNanos = 0L
    private lateinit var drawer: ICanvasDrawer
    private var renderVersion by mutableIntStateOf(0)
    private var contentWidth by mutableIntStateOf(0)
    private var contentHeight by mutableIntStateOf(0)
    private var isSetup = false

    private val lookupMatrix = Matrix()
    private var hapticFeedback: HapticFeedback? = null

    override val layerManager = MapViewLayerManager {
        invalidate()
    }
    private val hooks = Hooks()

    private var onLongPressCallback: ((Coordinate) -> Unit)? = null
    private var onSingleTapCallback: ((Coordinate) -> Unit)? = null
    private var pendingScaleChangeCallback = false
    private var onScaleChange: ((resolutionPixels: Float) -> Unit)? = null
    private var onCenterChange: ((center: Coordinate) -> Unit)? = null
    private val pendingLayoutCallbacks = mutableListOf<() -> Unit>()

    override var userLocation: Coordinate = Coordinate.zero
        set(value) {
            field = value
            invalidate()
        }

    override var userLocationAccuracy: Distance? = null
        set(value) {
            field = value
            invalidate()
        }

    override var userAzimuth: Bearing = Bearing.from(0f)
        set(value) {
            field = value
            invalidate()
        }

    val width: Int
        get() = contentWidth

    val height: Int
        get() = contentHeight

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        MapContent(modifier)
    }

    fun post(action: () -> Unit): Boolean {
        action()
        return true
    }

    fun invalidate() {
        post {
            renderVersion++
        }
    }

    fun doOnLayout(action: () -> Unit) {
        if (width > 0 && height > 0) {
            action()
        } else {
            pendingLayoutCallbacks.add(action)
        }
    }

    fun setOnLongPressListener(callback: ((Coordinate) -> Unit)?) {
        onLongPressCallback = callback
    }

    fun setOnSingleTapListener(callback: ((Coordinate) -> Unit)?) {
        onSingleTapCallback = callback
    }

    fun setBackgroundColor(color: Int) {
        backgroundColorOverride = color
        invalidate()
    }

    /** Sizes the map when it draws off screen (a home-screen widget's bitmap). */
    fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        contentWidth = right - left
        contentHeight = bottom - top
    }

    fun draw(canvas: AndroidCanvas) {
        if (::drawer.isInitialized) {
            drawer.canvas = canvas
        } else {
            drawer = CanvasDrawer(context, canvas)
        }
        setupIfNeeded()
        render()
    }

    override val mapBounds: CoordinateBounds
        get() = hooks.memo(
            "bounds",
            resolutionPixels, mapCenter, width, height, mapAzimuth != 0f,
            projection, metersPerProjectedUnit, latitudeScaleFactor
        ) {
            // Increase size to account for 45 degree rotation
            var rotated = Rectangle(
                0f,
                height.toFloat(),
                width.toFloat(),
                0f,
            )

            if (mapAzimuth != 0f) {
                rotated = rotated.rotate(45f)
            }

            val corners = listOf(
                toCoordinate(PixelCoordinate(rotated.left, rotated.bottom)),
                toCoordinate(PixelCoordinate(rotated.right, rotated.bottom)),
                toCoordinate(PixelCoordinate(rotated.left, rotated.top)),
                toCoordinate(PixelCoordinate(rotated.right, rotated.top))
            )
            val bounds = CoordinateBounds.from(corners)
            bounds.copy(
                north = bounds.north.coerceIn(-85.0, 85.0),
                south = bounds.south.coerceIn(-85.0, 85.0)
            )
        }

    override var resolution: Float
        get() = resolutionPixels * density
        set(value) {
            resolutionPixels = value / density
        }

    override val zoom: Float
        get() = TileMath.getZoomLevel(
            mapCenter,
            if (useDensityPixelsForZoom) resolution else resolutionPixels
        )

    override var resolutionPixels: Float
        get() {
            return (1f / scale) * latitudeScaleFactor(mapCenter.latitude)
        }
        set(value) {
            zoomTo(getScale(value / latitudeScaleFactor(mapCenter.latitude)))
        }

    private val equatorialResolutionPixels: Float
        get() = 1f / scale

    override val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    override var mapCenter: Coordinate = Coordinate.zero
        set(value) {
            field = constrainToBounds(value)
            onCenterChange?.invoke(field)
            invalidate()
        }

    private val mapCenterPixels: Vector2
        get() = hooks.memo("map_center_pixels", mapCenter, projection) {
            projection.toPixels(mapCenter)
        }

    override var mapAzimuth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    override val mapRotation: Float = 0f

    var scale = 1f
        private set(value) {
            field = value
            isScaleExplicitlySet = true
            onScaleChange?.invoke(resolutionPixels)
            invalidate()
        }
    private var lastScale = 1f
    private var isScaleExplicitlySet = false
    var minScale = 0.0001f
        set(value) {
            field = value
            if (scale < minScale) {
                zoomTo(minScale)
            }
            invalidate()
        }

    var constraintBounds: CoordinateBounds = CoordinateBounds(85.0, 180.0, -85.0, -180.0)
        set(value) {
            field = value
            if (!field.contains(mapCenter)) {
                mapCenter = field.center
            }
            invalidate()
        }

    var projection: IMapProjection = MercatorProjection()
        set(value) {
            field = value
            layerManager.invalidate()
            invalidate()
        }

    var metersPerProjectedUnit: Double = TileMath.WEB_MERCATOR_RADIUS
        set(value) {
            field = value
            layerManager.invalidate()
            invalidate()
        }

    var latitudeScaleFactor: (Double) -> Float = { MercatorProjection.getScaleForLatitude(it) }
        set(value) {
            field = value
            layerManager.invalidate()
            invalidate()
        }


    private val maxScale: Float
        get() = max(2 * minScale, getScale(MIN_RESOLUTION_PIXELS))

    private var isScaling = false

    private var fitToViewBounds: CoordinateBounds? = null
    private var fitToViewPadding: Float = 1f

    override val mapProjection: IMapViewProjection
        get() = hooks.memo(
            "mapProjection",
            mapCenter,
            projection,
            scale,
            width,
            height,
            useDensityPixelsForZoom,
            metersPerProjectedUnit,
            latitudeScaleFactor
        ) {
            val mapCenter = mapCenter
            val center = mapCenterPixels
            val projection = projection
            val equatorialResolutionPixels = equatorialResolutionPixels
            val metersPerProjectedUnit = metersPerProjectedUnit
            val groundResolutionPixels =
                equatorialResolutionPixels * latitudeScaleFactor(mapCenter.latitude)
            val width = width
            val height = height
            val zoom = zoom
            val resolution = resolution

            object : IMapProjection, IMapViewProjection {
                override fun toPixels(
                    latitude: Double,
                    longitude: Double
                ): PixelCoordinate {


                    // Always render the hemispheres closest to the map center
                    val projected = projection.toPixels(
                        latitude, mapCenter.longitude + deltaAngle(
                            mapCenter.longitude.toFloat(),
                            longitude.toFloat()
                        )
                    )

                    val x =
                        (projected.x - center.x) * (metersPerProjectedUnit / equatorialResolutionPixels)
                    val y =
                        (center.y - projected.y) * (metersPerProjectedUnit / equatorialResolutionPixels) // Y inverted

                    return PixelCoordinate(
                        x.toFloat() + width / 2f,
                        y.toFloat() + height / 2f
                    )
                }

                override fun toCoordinate(pixel: Vector2): Coordinate {
                    val x =
                        (pixel.x - width / 2f) * equatorialResolutionPixels / metersPerProjectedUnit
                    val y =
                        (height / 2f - pixel.y) * equatorialResolutionPixels / metersPerProjectedUnit // Y inverted

                    val projected = Vector2(
                        center.x + x.toFloat(),
                        center.y + y.toFloat()
                    )
                    return projection.toCoordinate(projected)
                }

                override fun toPixels(location: Coordinate): PixelCoordinate {
                    return toPixels(location.latitude, location.longitude)
                }

                override val resolutionPixels: Float = groundResolutionPixels
                override val resolution: Float = resolution
                override val zoom: Float = zoom
                override val center: Coordinate = mapCenter
            }
        }

    @Composable
    fun MapContent(modifier: Modifier = Modifier) {
        TrailSenseTheme {
            if (isInvisible) {
                return@TrailSenseTheme
            }
            hapticFeedback = LocalHapticFeedback.current
            Canvas(
                modifier = modifier
                    .fillMaxSize()
                    .then(if (keepScreenOn) Modifier.keepScreenOn() else Modifier)
                    // Taps first, then the drag loop inside it: a drag consumes its moves, which cancels a
                    // pending tap or long press, as Android's gesture detector did.
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { if (isInteractive) zoomWithFocus(2f, PixelCoordinate(it.x, it.y)) },
                            onTap = { if (isInteractive) onSingleTap(it) },
                            onLongPress = { if (isInteractive) onLongPress(it) }
                        )
                    }
                    .pointerInput(Unit) { detectMapDrags(viewConfiguration.touchSlop) }
                    .onSizeChanged {
                        contentWidth = it.width
                        contentHeight = it.height
                        if (it.width > 0 && it.height > 0 && pendingLayoutCallbacks.isNotEmpty()) {
                            val callbacks = pendingLayoutCallbacks.toList()
                            pendingLayoutCallbacks.clear()
                            callbacks.forEach { callback -> callback() }
                        }
                        this@MapView.layerManager.invalidate()
                    }
            ) {
                renderVersion
                if (::drawer.isInitialized) {
                    drawer.canvas = drawContext.canvas.nativeCanvas
                } else {
                    drawer = CanvasDrawer(context, drawContext.canvas.nativeCanvas)
                }
                setupIfNeeded()
                render()
            }
        }
    }

    private fun setupIfNeeded() {
        if (isSetup) {
            return
        }
        if (!isScaleExplicitlySet) {
            recenter()
        }
        isSetup = true
    }

    private fun render() {
        advanceFling()

        drawer.clear()
        drawer.push()
        clipPath?.let { drawer.clip(it) }
        backgroundColorOverride?.let { drawer.background(it) }

        zoomTo(clampScale(scale))

        if (pendingScaleChangeCallback) {
            pendingScaleChangeCallback = false
            onScaleChange?.invoke(resolutionPixels)
        }

        drawRotatedLayers()
        drawer.pop()
        layerManager.drawOverlay(context, drawer, this)

        if (isLayerDragging && layerManager.isMagnifierVisible) {
            magnifier.draw(drawer, layerTouch, ::drawRotatedLayers)
        }
    }

    private fun drawRotatedLayers() {
        drawer.push()
        drawer.rotate(-mapAzimuth)
        drawLayers()
        drawer.pop()
    }

    private fun drawLayers() {
        if (scale != lastScale) {
            lastScale = scale
            layerManager.invalidate()
        }
        // TODO: If map bounds changed, invalidate layers

        layerManager.draw(context, drawer, this)
    }

    private fun getScale(resolutionPixels: Float): Float {
        return 1f / resolutionPixels
    }

    fun recenter() {
        val fitToViewBounds = fitToViewBounds
        if (fitToViewBounds == null) {
            scale = 1f
        } else {
            fitIntoView(fitToViewBounds, fitToViewPadding)
        }
        invalidate()
    }

    // TODO: This doesn't work for world maps
    fun fitIntoView(bounds: CoordinateBounds, paddingFactor: Float = 1.25f) {
        if (width == 0 || height == 0) {
            return
        }
        fitToViewBounds = bounds
        fitToViewPadding = paddingFactor

        if (bounds == CoordinateBounds.world) {
            mapCenter = bounds.center
            zoomTo(minScale)
            invalidate()
            return
        }

        Optimization.newtonRaphsonIteration(scale, 0.001f, 10) {
            mapCenter = bounds.center
            val corners = listOf(
                bounds.northEast,
                bounds.southEast,
                bounds.northWest,
                bounds.southWest
            ).map { toPixel(constrainToBounds(it)) }
            val boxWidth = (corners.maxOf { it.x } - corners.minOf { it.x }).absoluteValue * paddingFactor
            val boxHeight = (corners.maxOf { it.y } - corners.minOf { it.y }).absoluteValue * paddingFactor

            if (boxWidth == 0f || boxHeight == 0f) {
                return@newtonRaphsonIteration 0f
            }

            val scaleX = width / boxWidth
            val scaleY = height / boxHeight
            val newScale = min(scaleX, scaleY)
            zoom(newScale)
            newScale
        }
        invalidate()
    }

    private fun zoomTo(newScale: Float) {
        if (newScale == scale) {
            return
        }
        zoom(newScale / scale)
    }

    fun zoom(factor: Float) {
        scale = clampScale(scale * factor)
        invalidate()
    }

    private fun clampScale(scale: Float): Float {
        return scale.coerceIn(minScale, maxScale)
    }

    private fun rotateByAzimuth(x: Float, y: Float): Vector2 {
        val angle = mapAzimuth
        return Vector2(
            x * cosDegrees(angle) - y * sinDegrees(angle),
            x * sinDegrees(angle) + y * cosDegrees(angle)
        )
    }

    private fun translatePixels(distanceX: Float, distanceY: Float) {
        val newPoint = PixelCoordinate(width / 2f + distanceX, height / 2f + distanceY)
        mapCenter = toCoordinate(newPoint)
    }

    private fun onSingleTap(position: Offset) {
        val pixel = unrotated(PixelCoordinate(position.x, position.y))
        if (::drawer.isInitialized) {
            layerManager.onClick(drawer, this@MapView, pixel)
        }
        onSingleTapCallback?.invoke(toCoordinate(pixel))
    }

    private fun onLongPress(position: Offset) {
        if (isScaling) return
        val pixel = unrotated(PixelCoordinate(position.x, position.y))
        if (::drawer.isInitialized && layerManager.onLongPress(drawer, this@MapView, pixel)) {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }
        onLongPressCallback?.invoke(toCoordinate(pixel))
    }

    /**
     * One touch at a time: a touch that starts on a draggable layer drags the layer; otherwise one finger pans,
     * two pinch-zoom around their centre, and a quick release flings. Moves are consumed only once they pass the
     * touch slop, so a still press stays a tap or a long press.
     */
    private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectMapDrags(touchSlop: Float) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!isInteractive) return@awaitEachGesture
            stopFling()
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            isLayerDragging = startLayerDrag(down.position)
            var pastSlop = false
            var travelled = Offset.Zero
            var lastPosition = down.position
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                val pan = event.calculatePan()
                val zoom = event.calculateZoom()
                if (!pastSlop) {
                    travelled += pan
                    pastSlop = travelled.getDistance() > touchSlop || (pressed.size > 1 && zoom != 1f)
                }
                lastPosition = pressed.first().position
                if (isLayerDragging) {
                    dragLayer(lastPosition)
                } else if (pastSlop) {
                    isScaling = pressed.size > 1
                    if (zoom != 1f) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        zoomWithFocus(zoom, PixelCoordinate(centroid.x, centroid.y))
                    }
                    if (isPanEnabled && pan != Offset.Zero) {
                        // The map follows the finger: moving content right means looking further left.
                        val delta = rotateByAzimuth(-pan.x, -pan.y)
                        translatePixels(delta.x, delta.y)
                    }
                }
                if (pastSlop) {
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
                if (pressed.size == 1) {
                    tracker.addPosition(pressed.first().uptimeMillis, pressed.first().position)
                } else {
                    tracker.resetTracking()
                }
            }
            if (isLayerDragging) {
                endLayerDrag(lastPosition)
            } else if (pastSlop && !isScaling && isPanEnabled && isFlingEnabled) {
                val velocity = tracker.calculateVelocity()
                val rotated = rotateByAzimuth(velocity.x, velocity.y)
                startFling(Offset(rotated.x, rotated.y) * FLING_VELOCITY_SCALE)
            }
            isScaling = false
        }
    }

    private fun startFling(velocity: Offset) {
        flingVelocity = velocity
        lastFlingFrameNanos = System.nanoTime()
        invalidate()
    }

    private fun stopFling() {
        flingVelocity = Offset.Zero
    }

    private fun advanceFling() {
        if (flingVelocity == Offset.Zero) return
        val now = System.nanoTime()
        val seconds = ((now - lastFlingFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFlingFrameNanos = now
        val moved = flingVelocity * seconds
        translatePixels(-moved.x, -moved.y)
        flingVelocity *= kotlin.math.exp(-FLING_FRICTION * seconds)
        if (flingVelocity.getDistance() < FLING_STOP_SPEED) {
            flingVelocity = Offset.Zero
        } else {
            invalidate()
        }
    }

    private fun zoomWithFocus(scaleFactor: Float, focus: PixelCoordinate) {
        if (!isZoomEnabled) return

        // Calculate the focus coordinate before zooming
        val unrotatedFocus = unrotated(focus)
        val focusCoordinate = toCoordinate(unrotatedFocus)

        zoom(scaleFactor)

        if (isPanEnabled) {
            // Keep the focus point stationary
            val newFocusPixel = toPixel(focusCoordinate)
            val dx = unrotatedFocus.x - newFocusPixel.x
            val dy = unrotatedFocus.y - newFocusPixel.y
            translatePixels(-dx, -dy)
        }
    }

    private val magnifier = MapMagnifier()
    private var isLayerDragging = false
    private var layerTouch = PixelCoordinate(0f, 0f)

    /** Lets a layer capture a touch that starts on it, so the touch drags something in the layer. */
    private fun startLayerDrag(position: Offset): Boolean {
        if (!::drawer.isInitialized) return false
        layerTouch = PixelCoordinate(position.x, position.y)
        val dragging = layerManager.onDragStart(drawer, this, unrotated(layerTouch))
        if (dragging) invalidate()
        return dragging
    }

    private fun dragLayer(position: Offset) {
        layerTouch = PixelCoordinate(position.x, position.y)
        layerManager.onDrag(drawer, this, unrotated(layerTouch))
        invalidate()
    }

    private fun endLayerDrag(position: Offset) {
        layerTouch = PixelCoordinate(position.x, position.y)
        isLayerDragging = false
        layerManager.onDragEnd(drawer, this, unrotated(layerTouch))
        invalidate()
    }

    private fun unrotated(pixel: PixelCoordinate): PixelCoordinate {
        val point = floatArrayOf(pixel.x, pixel.y)
        synchronized(lookupMatrix) {
            lookupMatrix.setRotate(mapAzimuth, width / 2f, height / 2f)
            lookupMatrix.mapPoints(point)
        }
        return PixelCoordinate(point[0], point[1])
    }

    fun setOnScaleChangeListener(
        invokeOnNextRender: Boolean = false,
        callback: ((resolutionPixels: Float) -> Unit)?
    ) {
        pendingScaleChangeCallback = invokeOnNextRender
        onScaleChange = callback
    }

    fun setOnCenterChangeListener(callback: ((center: Coordinate) -> Unit)?) {
        onCenterChange = callback
    }

    override fun setOnGeoJsonFeatureClickListener(listener: ((GeoJsonFeature) -> Unit)?) {
        layerManager.setOnGeoJsonFeatureClickListener(listener)
    }

    private fun constrainToBounds(location: Coordinate): Coordinate {
        return Coordinate(
            location.latitude.coerceIn(constraintBounds.south, constraintBounds.north),
            Coordinate.toLongitude(location.longitude).coerceIn(
                min(constraintBounds.west, constraintBounds.east),
                max(constraintBounds.west, constraintBounds.east)
            )
        )
    }

    companion object {
        private const val FLING_VELOCITY_SCALE = 0.6f
        private const val FLING_FRICTION = 3.5f
        private const val FLING_STOP_SPEED = 30f
        private const val MIN_RESOLUTION_PIXELS = 0.1f
    }
}
