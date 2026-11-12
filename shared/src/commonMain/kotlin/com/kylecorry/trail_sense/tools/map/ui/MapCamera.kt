package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.optimization.Optimization
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.science.geography.projections.IMapProjection
import com.kylecorry.sol.science.geography.projections.MercatorProjection
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Where a map looks and how close: its center, scale and rotation on a canvas of a given size, and the projection
 * between coordinates and pixels that follows from them. Every field is observable state, so whatever draws the map
 * redraws when the camera moves.
 *
 * @param density pixels per density-independent pixel, for [resolution] and density-based zoom levels
 */
class MapCamera(private val density: Float) {

    /** The canvas size in pixels. */
    var width by mutableIntStateOf(0)
        private set
    var height by mutableIntStateOf(0)
        private set

    var useDensityPixelsForZoom by mutableStateOf(true)
    var isPanEnabled by mutableStateOf(true)
    var isZoomEnabled by mutableStateOf(true)
    var isFlingEnabled by mutableStateOf(true)

    /** Called with the new center whenever it changes. */
    var onCenterChange: ((Coordinate) -> Unit)? = null

    /** Called with the new resolution (meters per pixel) whenever the scale changes. */
    var onScaleChange: ((resolutionPixels: Float) -> Unit)? = null

    private var _center by mutableStateOf(Coordinate.zero)
    var center: Coordinate
        get() = _center
        set(value) {
            _center = constrainToBounds(value)
            onCenterChange?.invoke(_center)
        }

    /** Degrees the map is turned clockwise, so that this bearing points up. */
    var azimuth by mutableFloatStateOf(0f)

    private var _scale by mutableFloatStateOf(1f)

    /** Pixels per projected meter at the equator. */
    var scale: Float
        get() = _scale
        private set(value) {
            _scale = value
            isScaleExplicitlySet = true
            onScaleChange?.invoke(resolutionPixels)
        }

    /** Whether something has set the scale, so a first layout should not reset it. */
    var isScaleExplicitlySet = false
        private set

    private var _minScale by mutableFloatStateOf(0.0001f)
    var minScale: Float
        get() = _minScale
        set(value) {
            _minScale = value
            if (scale < value) {
                zoomTo(value)
            }
        }

    private var _constraintBounds by mutableStateOf(CoordinateBounds(85.0, 180.0, -85.0, -180.0))
    var constraintBounds: CoordinateBounds
        get() = _constraintBounds
        set(value) {
            _constraintBounds = value
            if (!value.contains(center)) {
                center = value.center
            }
        }

    var projection: IMapProjection by mutableStateOf(MercatorProjection())
    var metersPerProjectedUnit: Double by mutableStateOf(TileMath.WEB_MERCATOR_RADIUS)
    var latitudeScaleFactor: (Double) -> Float by mutableStateOf({ MercatorProjection.getScaleForLatitude(it) })

    private val maxScale: Float
        get() = max(2 * minScale, 1f / MIN_RESOLUTION_PIXELS)

    private var fitToViewBounds: CoordinateBounds? = null
    private var fitToViewPadding: Float = 1f

    /** Meters per pixel at the map's center. */
    var resolutionPixels: Float
        get() = (1f / scale) * latitudeScaleFactor(center.latitude)
        set(value) {
            zoomTo(1f / (value / latitudeScaleFactor(center.latitude)))
        }

    /** Meters per density-independent pixel at the map's center. */
    var resolution: Float
        get() = resolutionPixels * density
        set(value) {
            resolutionPixels = value / density
        }

    val zoom: Float
        get() = TileMath.getZoomLevel(center, if (useDensityPixelsForZoom) resolution else resolutionPixels)

    /** How large layer elements draw: shrinking a little as the map zooms out, never growing past full size. */
    val layerScale: Float
        get() = min(1f, max(scale, 0.9f))

    private var cachedProjection: Pair<List<Any?>, IMapViewProjection>? = null

    /** The projection for the camera as it is now. It never changes after it is returned. */
    val mapProjection: IMapViewProjection
        get() {
            val key = listOf(
                center, projection, scale, width, height, useDensityPixelsForZoom, metersPerProjectedUnit,
                latitudeScaleFactor
            )
            cachedProjection?.let { (cachedKey, value) -> if (cachedKey == key) return value }
            return createProjection().also { cachedProjection = key to it }
        }

    private var cachedBounds: Pair<List<Any?>, CoordinateBounds>? = null

    /** The area the canvas shows, grown to cover any rotation. */
    val bounds: CoordinateBounds
        get() {
            val key = listOf(mapProjection, azimuth != 0f)
            cachedBounds?.let { (cachedKey, value) -> if (cachedKey == key) return value }
            return computeBounds().also { cachedBounds = key to it }
        }

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun toPixel(coordinate: Coordinate): PixelCoordinate = mapProjection.toPixels(coordinate)

    fun toCoordinate(pixel: PixelCoordinate): Coordinate = mapProjection.toCoordinate(pixel)

    /** The point on the unrotated map under [pixel] on the (rotated) screen. */
    fun unrotated(pixel: PixelCoordinate): PixelCoordinate {
        val rotated = rotateByAzimuth(pixel.x - width / 2f, pixel.y - height / 2f)
        return PixelCoordinate(rotated.x + width / 2f, rotated.y + height / 2f)
    }

    fun zoom(factor: Float) {
        scale = clampScale(scale * factor)
    }

    fun zoomTo(newScale: Float) {
        if (newScale != scale) {
            zoom(newScale / scale)
        }
    }

    /** Keeps the scale within the allowed range, for example after the minimum scale changes. */
    fun clamp() {
        zoomTo(clampScale(scale))
    }

    /** Zooms by [factor], keeping the map under [focus] (a screen pixel) in place. */
    fun zoomWithFocus(factor: Float, focus: PixelCoordinate) {
        if (!isZoomEnabled) return
        val unrotatedFocus = unrotated(focus)
        val focusCoordinate = toCoordinate(unrotatedFocus)
        zoom(factor)
        if (isPanEnabled) {
            val newFocusPixel = toPixel(focusCoordinate)
            translatePixels(newFocusPixel.x - unrotatedFocus.x, newFocusPixel.y - unrotatedFocus.y)
        }
    }

    /** Moves the map with a finger that moved by ([dx], [dy]) screen pixels. */
    fun pan(dx: Float, dy: Float) {
        if (!isPanEnabled) return
        val delta = rotateByAzimuth(-dx, -dy)
        translatePixels(delta.x, delta.y)
    }

    /**
     * Glides the map on after a finger lifts at [velocity] (screen pixels per second), slowing exponentially until it
     * stops or the calling coroutine is cancelled.
     */
    suspend fun fling(velocityX: Float, velocityY: Float) {
        if (!isPanEnabled || !isFlingEnabled) return
        val rotated = rotateByAzimuth(velocityX, velocityY)
        var vx = rotated.x * FLING_VELOCITY_SCALE
        var vy = rotated.y * FLING_VELOCITY_SCALE
        var last = withFrameNanos { it }
        while (hypot(vx, vy) >= FLING_STOP_SPEED) {
            val now = withFrameNanos { it }
            val seconds = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = now
            translatePixels(-vx * seconds, -vy * seconds)
            val decay = exp(-FLING_FRICTION * seconds)
            vx *= decay
            vy *= decay
        }
    }

    /** Resets to the bounds last fitted, or to full scale. */
    fun recenter() {
        val bounds = fitToViewBounds
        if (bounds == null) {
            scale = 1f
        } else {
            fitIntoView(bounds, fitToViewPadding)
        }
    }

    /** Centers and scales the map so [bounds] fills the canvas, with [paddingFactor] to spare. */
    fun fitIntoView(bounds: CoordinateBounds, paddingFactor: Float = 1.25f) {
        if (width == 0 || height == 0) {
            return
        }
        fitToViewBounds = bounds
        fitToViewPadding = paddingFactor

        if (bounds == CoordinateBounds.world) {
            center = bounds.center
            zoomTo(minScale)
            return
        }

        Optimization.newtonRaphsonIteration(scale, 0.001f, 10) {
            center = bounds.center
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

            val newScale = min(width / boxWidth, height / boxHeight)
            zoom(newScale)
            newScale
        }
    }

    private fun clampScale(scale: Float): Float = scale.coerceIn(minScale, maxScale)

    private fun rotateByAzimuth(x: Float, y: Float): Vector2 {
        val angle = azimuth
        return Vector2(
            x * cosDegrees(angle) - y * sinDegrees(angle),
            x * sinDegrees(angle) + y * cosDegrees(angle)
        )
    }

    private fun translatePixels(distanceX: Float, distanceY: Float) {
        center = toCoordinate(PixelCoordinate(width / 2f + distanceX, height / 2f + distanceY))
    }

    private fun constrainToBounds(location: Coordinate): Coordinate {
        val bounds = constraintBounds
        return Coordinate(
            location.latitude.coerceIn(bounds.south, bounds.north),
            Coordinate.toLongitude(location.longitude).coerceIn(
                min(bounds.west, bounds.east),
                max(bounds.west, bounds.east)
            )
        )
    }

    private fun computeBounds(): CoordinateBounds {
        // Grown to cover a 45 degree rotation
        var rotated = Rectangle(0f, height.toFloat(), width.toFloat(), 0f)
        if (azimuth != 0f) {
            rotated = rotated.rotate(45f)
        }
        val corners = listOf(
            toCoordinate(PixelCoordinate(rotated.left, rotated.bottom)),
            toCoordinate(PixelCoordinate(rotated.right, rotated.bottom)),
            toCoordinate(PixelCoordinate(rotated.left, rotated.top)),
            toCoordinate(PixelCoordinate(rotated.right, rotated.top))
        )
        val bounds = CoordinateBounds.from(corners)
        return bounds.copy(
            north = bounds.north.coerceIn(-85.0, 85.0),
            south = bounds.south.coerceIn(-85.0, 85.0)
        )
    }

    private fun createProjection(): IMapViewProjection {
        val mapCenter = center
        val projection = projection
        val centerPixels = projection.toPixels(mapCenter)
        val equatorialResolutionPixels = 1f / scale
        val metersPerProjectedUnit = metersPerProjectedUnit
        val groundResolutionPixels = equatorialResolutionPixels * latitudeScaleFactor(mapCenter.latitude)
        val width = width
        val height = height
        val zoom = zoom
        val resolution = resolution

        return object : IMapProjection, IMapViewProjection {
            override fun toPixels(latitude: Double, longitude: Double): PixelCoordinate {
                // Always render the hemispheres closest to the map center
                val projected = projection.toPixels(
                    latitude,
                    mapCenter.longitude + deltaAngle(mapCenter.longitude.toFloat(), longitude.toFloat())
                )
                val x = (projected.x - centerPixels.x) * (metersPerProjectedUnit / equatorialResolutionPixels)
                // Y inverted
                val y = (centerPixels.y - projected.y) * (metersPerProjectedUnit / equatorialResolutionPixels)
                return PixelCoordinate(x.toFloat() + width / 2f, y.toFloat() + height / 2f)
            }

            override fun toCoordinate(pixel: Vector2): Coordinate {
                val x = (pixel.x - width / 2f) * equatorialResolutionPixels / metersPerProjectedUnit
                // Y inverted
                val y = (height / 2f - pixel.y) * equatorialResolutionPixels / metersPerProjectedUnit
                return projection.toCoordinate(Vector2(centerPixels.x + x.toFloat(), centerPixels.y + y.toFloat()))
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

    private companion object {
        const val FLING_VELOCITY_SCALE = 0.6f
        const val FLING_FRICTION = 3.5f
        const val FLING_STOP_SPEED = 30f
        const val MIN_RESOLUTION_PIXELS = 0.1f
    }
}
