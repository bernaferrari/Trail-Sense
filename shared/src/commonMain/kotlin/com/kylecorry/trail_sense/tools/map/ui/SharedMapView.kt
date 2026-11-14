package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import com.kylecorry.trail_sense.shared.canvas.ComposeMapDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

/**
 * Draws map layers through the shared [MapCamera], which owns where the map looks and moves it with [mapGestures].
 * Map state, gestures, and Compose rendering are shared across platforms.
 */
open class SharedMapView(private val initialDensity: Float, private val initialFontScale: Float = 1f) : IMapView {

    val camera = MapCamera(initialDensity)

    override var isWidget: Boolean = false
    var isInteractive = true
    var isPanEnabled by camera::isPanEnabled
    var isZoomEnabled by camera::isZoomEnabled
    var isFlingEnabled by camera::isFlingEnabled
    var useDensityPixelsForZoom by camera::useDensityPixelsForZoom
    var id: Int = 0
    var keepScreenOn by mutableStateOf(false)
    var isInvisible by mutableStateOf(false)
    override val isHighDetailMode: Boolean
        get() = !useDensityPixelsForZoom
    var clipPath: Path? by mutableStateOf(null)
    var backgroundColorOverride: Int? by mutableStateOf(null)

    private lateinit var drawer: ComposeMapDrawer

    /** Bumped when a layer or a caller asks for a redraw; the camera's own state redraws on its own. */
    private var renderVersion by mutableIntStateOf(0)
    private var isSetup = false
    private var hapticFeedback: HapticFeedback? = null

    override val layerManager = MapViewLayerManager {
        invalidate()
    }

    private var onLongPressCallback: ((Coordinate) -> Unit)? = null
    private var onSingleTapCallback: ((Coordinate) -> Unit)? = null
    private var pendingScaleChangeCallback = false
    private val pendingLayoutCallbacks = mutableListOf<() -> Unit>()

    override var userLocation: Coordinate by mutableStateOf(Coordinate.zero)
    override var userLocationAccuracy: Distance? by mutableStateOf(null)
    override var userAzimuth: Bearing by mutableStateOf(Bearing.from(0f))

    val width: Int
        get() = camera.width

    val height: Int
        get() = camera.height

    override val mapBounds: CoordinateBounds
        get() = camera.bounds

    override var resolution: Float by camera::resolution
    override val zoom: Float
        get() = camera.zoom
    override var resolutionPixels: Float by camera::resolutionPixels
    override val layerScale: Float
        get() = camera.layerScale
    override var mapCenter: Coordinate by camera::center
    override var mapAzimuth: Float by camera::azimuth
    override val mapRotation: Float = 0f
    val scale: Float
        get() = camera.scale
    var minScale: Float by camera::minScale
    var constraintBounds: CoordinateBounds by camera::constraintBounds

    var projection: IMapProjection
        get() = camera.projection
        set(value) {
            camera.projection = value
            layerManager.invalidate()
        }

    var metersPerProjectedUnit: Double
        get() = camera.metersPerProjectedUnit
        set(value) {
            camera.metersPerProjectedUnit = value
            layerManager.invalidate()
        }

    var latitudeScaleFactor: (Double) -> Float
        get() = camera.latitudeScaleFactor
        set(value) {
            camera.latitudeScaleFactor = value
            layerManager.invalidate()
        }

    override val mapProjection: IMapViewProjection
        get() = camera.mapProjection

    private var lastScale = 1f
    private val magnifier = MapMagnifier()
    private var isLayerDragging = false
    private var layerTouch = PixelCoordinate(0f, 0f)

    private val gestureHandler = object : MapGestureHandler {
        override val isInteractive: Boolean
            get() = this@SharedMapView.isInteractive

        override fun onTap(position: PixelCoordinate) {
            val pixel = camera.unrotated(position)
            if (::drawer.isInitialized) {
                layerManager.onClick(drawer, this@SharedMapView, pixel)
            }
            onSingleTapCallback?.invoke(camera.toCoordinate(pixel))
        }

        override fun onLongPress(position: PixelCoordinate) {
            val pixel = camera.unrotated(position)
            if (::drawer.isInitialized && layerManager.onLongPress(drawer, this@SharedMapView, pixel)) {
                hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
                return
            }
            onLongPressCallback?.invoke(camera.toCoordinate(pixel))
        }

        override fun onDragStart(position: PixelCoordinate): Boolean {
            if (!::drawer.isInitialized) return false
            layerTouch = position
            isLayerDragging = layerManager.onDragStart(drawer, this@SharedMapView, camera.unrotated(position))
            if (isLayerDragging) invalidate()
            return isLayerDragging
        }

        override fun onDrag(position: PixelCoordinate) {
            layerTouch = position
            layerManager.onDrag(drawer, this@SharedMapView, camera.unrotated(position))
            invalidate()
        }

        override fun onDragEnd(position: PixelCoordinate) {
            layerTouch = position
            isLayerDragging = false
            layerManager.onDragEnd(drawer, this@SharedMapView, camera.unrotated(position))
            invalidate()
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        MapContent(modifier)
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
                    .mapGestures(camera, gestureHandler)
                    .onSizeChanged {
                        camera.setSize(it.width, it.height)
                        runPendingLayoutCallbacks()
                        layerManager.invalidate()
                    }
            ) {
                renderVersion
                if (!::drawer.isInitialized) {
                    drawer = ComposeMapDrawer(density, fontScale)
                }
                drawer.setCanvas(drawContext.canvas, size.width, size.height)
                setupIfNeeded()
                render()
            }
        }
    }

    fun post(action: () -> Unit): Boolean {
        action()
        return true
    }

    fun invalidate() {
        renderVersion++
    }

    fun doOnLayout(action: () -> Unit) {
        if (width > 0 && height > 0) {
            action()
        } else {
            pendingLayoutCallbacks.add(action)
        }
    }

    private fun runPendingLayoutCallbacks() {
        if (width > 0 && height > 0 && pendingLayoutCallbacks.isNotEmpty()) {
            val callbacks = pendingLayoutCallbacks.toList()
            pendingLayoutCallbacks.clear()
            callbacks.forEach { it() }
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
    }

    /** Sizes the map when it draws off screen (a home-screen widget's bitmap). */
    fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        camera.setSize(right - left, bottom - top)
    }

    /** Draws the map into an existing Compose canvas, including off-screen render targets. */
    fun draw(canvas: ComposeCanvas, width: Int, height: Int) {
        if (!::drawer.isInitialized) {
            drawer = ComposeMapDrawer(initialDensity, initialFontScale)
        }
        drawer.setCanvas(canvas, width.toFloat(), height.toFloat())
        setupIfNeeded()
        render()
    }

    private fun setupIfNeeded() {
        if (isSetup) {
            return
        }
        if (!camera.isScaleExplicitlySet) {
            camera.recenter()
        }
        isSetup = true
    }

    private fun render() {
        drawer.clear()
        drawer.push()
        clipPath?.let { drawer.clip(it) }
        backgroundColorOverride?.let { drawer.background(it) }

        camera.clamp()

        if (pendingScaleChangeCallback) {
            pendingScaleChangeCallback = false
            camera.onScaleChange?.invoke(resolutionPixels)
        }

        drawRotatedLayers()
        drawer.pop()
        layerManager.drawOverlay(drawer, this)

        if (isLayerDragging && layerManager.isMagnifierVisible) {
            magnifier.draw(drawer, layerTouch, ::drawRotatedLayers)
        }
    }

    private fun drawRotatedLayers() {
        drawer.push()
        drawer.rotate(-mapAzimuth)
        if (scale != lastScale) {
            lastScale = scale
            layerManager.invalidate()
        }
        layerManager.draw(drawer, this)
        drawer.pop()
    }

    fun recenter() {
        camera.recenter()
    }

    fun fitIntoView(bounds: CoordinateBounds, paddingFactor: Float = 1.25f) {
        camera.fitIntoView(bounds, paddingFactor)
    }

    fun zoom(factor: Float) {
        camera.zoom(factor)
    }

    fun setOnScaleChangeListener(
        invokeOnNextRender: Boolean = false,
        callback: ((resolutionPixels: Float) -> Unit)?
    ) {
        pendingScaleChangeCallback = invokeOnNextRender
        camera.onScaleChange = callback
    }

    fun setOnCenterChangeListener(callback: ((center: Coordinate) -> Unit)?) {
        camera.onCenterChange = callback
    }

    override fun setOnGeoJsonFeatureClickListener(listener: ((GeoJsonFeature) -> Unit)?) {
        layerManager.setOnGeoJsonFeatureClickListener(listener)
    }
}
