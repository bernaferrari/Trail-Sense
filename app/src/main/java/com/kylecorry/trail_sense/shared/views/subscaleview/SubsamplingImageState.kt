package com.kylecorry.trail_sense.shared.views.subscaleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A large image shown in tiles, the Compose replacement for SubsamplingScaleImageView.
 *
 * The whole image is kept as a low resolution base layer and the visible part is decoded in tiles at the resolution
 * the current scale needs, so very large images (or PDF pages) stay sharp without holding every pixel in memory.
 *
 * Coordinates: the *source* is the image after its [orientation] (a quarter turn rotation) is applied, in image
 * pixels; the *view* is the composable in pixels. [scale] is view pixels per source pixel and [translateX] /
 * [translateY] is where the source origin sits in the view. [rotation] then turns the whole view (image and anything
 * drawn over it) about its center; [sourceToView] / [viewToSource] ignore it, like the view they replace did.
 *
 * Draw it with [SubsamplingImage]; it is a rendering object, so keep it in the composition (`remember`).
 */
@Stable
class SubsamplingImageState(context: Context) {

    private val context = context.applicationContext

    /** Read while drawing so the canvas redraws when the image moves or a tile arrives. */
    internal var frame by mutableIntStateOf(0)
        private set

    // Source
    private var spec: ImageTileSourceSpec? = null
    private var source: ImageTileSource? = null
    private var sourceJob: Job? = null
    private var scope: CoroutineScope? = null

    /** The stored image's width, before [orientation]. */
    var rawWidth = 0
        private set

    /** The stored image's height, before [orientation]. */
    var rawHeight = 0
        private set

    /** A quarter turn (0, 90, 180 or 270 degrees clockwise) applied to the stored image. */
    var orientation = 0
        set(value) {
            if (field == value) return
            field = value
            // The tiles keep their file regions; only where they are drawn changes
            hasTransform = false
            applyPendingOrInitialTransform()
            invalidate()
        }

    /** The image's width after [orientation]. */
    val width: Int
        get() = if (orientation == 90 || orientation == 270) rawHeight else rawWidth

    /** The image's height after [orientation]. */
    val height: Int
        get() = if (orientation == 90 || orientation == 270) rawWidth else rawHeight

    // Viewport
    var viewWidth = 0
        private set
    var viewHeight = 0
        private set

    // Transform
    var scale = 0f
        private set
    var translateX = 0f
        private set
    var translateY = 0f
        private set

    /** Degrees the whole view is turned clockwise about its center. */
    var rotation = 0f
        set(value) {
            if (field == value) return
            field = value
            refreshTiles()
            invalidate()
        }

    private var hasTransform = false
    private var pendingScale: Float? = null
    private var pendingCenter: PointF? = null
    private var pendingReset = false
    private var requestedCenter: PointF? = null

    // Limits
    private var minScaleOverride: Float? = null

    /**
     * Shrinks the fitted minimum scale (given the image's width and height after [orientation]), e.g. to leave room
     * for the image to turn.
     */
    var minScaleFactor: (width: Float, height: Float) -> Float = { _, _ -> 1f }
        set(value) {
            field = value
            invalidate()
        }

    /** The smallest scale: set it, or by default the image fits inside the view (times [minScaleFactor]). */
    var minScale: Float
        get() = minScaleOverride ?: (fitScale() * fitFactor())
        set(value) {
            minScaleOverride = value
            invalidate()
        }

    var maxScale = 2f
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var isPanEnabled = true
    var isZoomEnabled = true

    /** Each double tap zooms in by this much. */
    var doubleTapZoomBy = 2f

    // Tiles
    private var tiles: Map<Int, List<Tile>> = emptyMap()
    private var fullImageSampleSize = 1
    private var currentSampleSize = 1

    /** Whether the whole image (the base layer) is decoded, so it can be drawn and positioned. */
    var isReady = false
        private set

    /** Whether the view and image size are known, so coordinates can be converted. */
    val hasLayout: Boolean
        get() = hasTransform

    private val readyListeners = mutableListOf<() -> Unit>()

    // Animation
    private var animationJob: Job? = null

    // Drawing
    private val matrix = Matrix()
    private val inverse = Matrix()
    private val bitmapPaint = Paint().apply {
        isFilterBitmap = true
        isDither = true
    }
    private val drawRect = RectF()

    fun addOnReadyListener(listener: () -> Unit) {
        readyListeners.add(listener)
    }

    /** Shows [spec]; nothing happens if it is already shown. */
    fun setSource(spec: ImageTileSourceSpec) {
        if (this.spec == spec) return
        this.spec = spec
        closeSource()
        rawWidth = 0
        rawHeight = 0
        hasTransform = false
        openSource()
        invalidate()
    }

    // Lifecycle (called by SubsamplingImage)

    /** [parent] must come from the composition (it carries the frame clock the animations run on). */
    internal fun attach(parent: CoroutineScope) {
        if (scope != null) return
        scope = CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job]))
        openSource()
    }

    internal fun detach() {
        closeSource()
        scope?.cancel()
        scope = null
    }

    private fun openSource() {
        val scope = scope ?: return
        val spec = spec ?: return
        sourceJob = scope.launch {
            try {
                val opened = ImageTileSource.open(context, spec)
                source = opened
                rawWidth = opened.width
                rawHeight = opened.height
                applyPendingOrInitialTransform()
                initializeTiles()
                invalidate()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Unable to open image", e)
            }
        }
    }

    private fun closeSource() {
        sourceJob?.cancel()
        sourceJob = null
        tiles.values.flatten().forEach { it.clear() }
        tiles = emptyMap()
        isReady = false
        val toClose = source
        source = null
        if (toClose != null) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { toClose.close() }
            }
        }
    }

    internal fun onSizeChanged(width: Int, height: Int) {
        if (width == viewWidth && height == viewHeight) return
        // Keep what is centered when the view resizes
        val center = if (hasTransform) getCenter() else null
        if (center != null && pendingCenter == null) {
            pendingCenter = center
            pendingScale = pendingScale ?: scale
        }
        viewWidth = width
        viewHeight = height
        hasTransform = false
        applyPendingOrInitialTransform()
        if (tiles.isEmpty()) {
            initializeTiles()
        }
        refreshTiles()
        invalidate()
    }

    fun invalidate() {
        frame++
    }

    // Coordinates

    fun sourceToView(sourceX: Float, sourceY: Float): PointF? {
        if (!hasTransform) return null
        return PointF(sourceX * scale + translateX, sourceY * scale + translateY)
    }

    fun viewToSource(viewX: Float, viewY: Float): PointF? {
        if (!hasTransform) return null
        return PointF((viewX - translateX) / scale, (viewY - translateY) / scale)
    }

    /** The source point at the center of the view. */
    fun getCenter(): PointF? {
        return viewToSource(viewWidth / 2f, viewHeight / 2f)
    }

    /** Turns a point in the view (as touched) into the view before [rotation] is applied. */
    internal fun unrotate(x: Float, y: Float): PointF {
        return rotate(x, y, -rotation)
    }

    private fun rotate(x: Float, y: Float, degrees: Float): PointF {
        if (degrees == 0f) return PointF(x, y)
        val cx = viewWidth / 2f
        val cy = viewHeight / 2f
        val radians = Math.toRadians(degrees.toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        val dx = x - cx
        val dy = y - cy
        return PointF(cx + dx * c - dy * s, cy + dx * s + dy * c)
    }

    // Requests (applied once the image and view sizes are known)

    fun setScaleAndCenter(scale: Float, sourceCenter: PointF?) {
        stopAnimation()
        pendingScale = scale
        pendingCenter = sourceCenter
        requestedCenter = sourceCenter
        applyPendingIfPossible()
    }

    fun requestScale(scale: Float) {
        stopAnimation()
        pendingScale = scale
        if (pendingCenter == null) {
            pendingCenter = getCenter()
        }
        applyPendingIfPossible()
    }

    fun requestCenter(sourceCenter: PointF?) {
        stopAnimation()
        pendingCenter = sourceCenter
        requestedCenter = sourceCenter
        if (pendingScale == null && hasTransform) {
            pendingScale = scale
        }
        applyPendingIfPossible()
    }

    fun resetScaleAndCenter() {
        stopAnimation()
        pendingReset = true
        pendingScale = null
        pendingCenter = null
        applyPendingIfPossible()
    }

    private fun canLayout(): Boolean {
        return viewWidth > 0 && viewHeight > 0 && rawWidth > 0 && rawHeight > 0
    }

    private fun applyPendingIfPossible() {
        if (!canLayout()) {
            return
        }
        if (!hasTransform) {
            applyPendingOrInitialTransform()
        } else {
            applyPending()
            refreshTiles()
        }
        invalidate()
    }

    private fun applyPendingOrInitialTransform() {
        if (!canLayout()) return
        if (!hasTransform) {
            // Start fitted and centered
            scale = minScale
            translateX = viewWidth / 2f - scale * width / 2f
            translateY = viewHeight / 2f - scale * height / 2f
            hasTransform = true
        }
        applyPending()
        refreshTiles()
    }

    private fun applyPending() {
        if (pendingReset) {
            pendingReset = false
            scale = minScale
            translateX = viewWidth / 2f - scale * width / 2f
            translateY = viewHeight / 2f - scale * height / 2f
        }
        val center = pendingCenter
        val newScale = pendingScale
        if (center != null || newScale != null) {
            val c = center ?: getCenter() ?: PointF(width / 2f, height / 2f)
            scale = limitScale(newScale ?: scale)
            translateX = viewWidth / 2f - scale * c.x
            translateY = viewHeight / 2f - scale * c.y
            pendingCenter = null
            pendingScale = null
        }
        fitToBounds()
    }

    // Limits

    private fun fitScale(): Float {
        if (viewWidth <= 0 || viewHeight <= 0 || width <= 0 || height <= 0) return 1f
        return min(viewWidth / width.toFloat(), viewHeight / height.toFloat())
    }

    private fun fitFactor(): Float {
        if (width <= 0 || height <= 0) return 1f
        return minScaleFactor(width.toFloat(), height.toFloat())
    }

    internal fun limitScale(scale: Float): Float {
        return min(maxScale, max(minScale, scale))
    }

    /** Keeps the image inside the view when it is larger, and centered on an axis where it is smaller. */
    internal fun fitToBounds() {
        scale = limitScale(scale)
        val scaledWidth = scale * width
        val scaledHeight = scale * height
        translateX = max(translateX, viewWidth - scaledWidth)
        translateY = max(translateY, viewHeight - scaledHeight)
        val maxX = max(0f, (viewWidth - scaledWidth) / 2f)
        val maxY = max(0f, (viewHeight - scaledHeight) / 2f)
        translateX = min(translateX, maxX)
        translateY = min(translateY, maxY)
    }

    // Gestures (in the view before rotation)

    internal fun panBy(dx: Float, dy: Float) {
        if (!hasTransform || !isPanEnabled) return
        translateX += dx
        translateY += dy
        onTransformChanged()
    }

    /** Scales by [zoom] keeping the view point ([focusX], [focusY]) over the same part of the image. */
    internal fun zoomBy(zoom: Float, focusX: Float, focusY: Float, panX: Float = 0f, panY: Float = 0f) {
        if (!hasTransform || !isZoomEnabled) return
        val newScale = limitScale(scale * zoom)
        if (isPanEnabled) {
            val actualZoom = newScale / scale
            translateX = focusX - (focusX - translateX) * actualZoom + panX
            translateY = focusY - (focusY - translateY) * actualZoom + panY
            scale = newScale
        } else {
            val center = requestedCenter ?: PointF(width / 2f, height / 2f)
            scale = newScale
            translateX = viewWidth / 2f - scale * center.x
            translateY = viewHeight / 2f - scale * center.y
        }
        onTransformChanged()
    }

    private fun onTransformChanged() {
        fitToBounds()
        refreshTiles()
        invalidate()
    }

    internal fun doubleTapZoom(viewX: Float, viewY: Float) {
        if (!hasTransform || !isZoomEnabled) return
        val target = max(min(scale * doubleTapZoomBy, maxScale), minScale)
        if (isPanEnabled) {
            val sourceCenter = viewToSource(viewX, viewY) ?: return
            animate(target, sourceCenter, PointF(viewX, viewY), DOUBLE_TAP_DURATION, ::easeInOutQuad)
        } else {
            val sourceCenter = requestedCenter ?: PointF(width / 2f, height / 2f)
            animate(target, sourceCenter, PointF(viewWidth / 2f, viewHeight / 2f), DOUBLE_TAP_DURATION, ::easeInOutQuad)
        }
    }

    internal fun fling(velocityX: Float, velocityY: Float) {
        if (!hasTransform || !isPanEnabled) return
        val endX = translateX + velocityX * FLING_DISTANCE
        val endY = translateY + velocityY * FLING_DISTANCE
        val sourceCenter = PointF(
            (viewWidth / 2f - endX) / scale,
            (viewHeight / 2f - endY) / scale
        )
        animate(scale, sourceCenter, PointF(viewWidth / 2f, viewHeight / 2f), FLING_DURATION, ::easeOutQuad)
    }

    /**
     * Moves [sourceFocus] to [viewFocus] while scaling to [targetScale]. The end is kept inside the bounds so the
     * image doesn't spring back once it stops.
     */
    private fun animate(
        targetScale: Float,
        sourceFocus: PointF,
        viewFocus: PointF,
        durationMillis: Long,
        easing: (Float) -> Float
    ) {
        val scope = scope ?: return
        stopAnimation()
        val startScale = scale
        val endScale = limitScale(targetScale)

        // Where the focus ends up once the end state is kept in bounds
        val savedScale = scale
        val savedX = translateX
        val savedY = translateY
        scale = endScale
        translateX = viewFocus.x - endScale * sourceFocus.x
        translateY = viewFocus.y - endScale * sourceFocus.y
        fitToBounds()
        val endFocus = PointF(sourceFocus.x * scale + translateX, sourceFocus.y * scale + translateY)
        scale = savedScale
        translateX = savedX
        translateY = savedY

        val startFocus = PointF(sourceFocus.x * scale + translateX, sourceFocus.y * scale + translateY)

        animationJob = scope.launch {
            val start = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val t = ((now - start) / 1_000_000f / durationMillis).coerceIn(0f, 1f)
                val e = easing(t)
                scale = startScale + (endScale - startScale) * e
                val focusX = startFocus.x + (endFocus.x - startFocus.x) * e
                val focusY = startFocus.y + (endFocus.y - startFocus.y) * e
                translateX = focusX - scale * sourceFocus.x
                translateY = focusY - scale * sourceFocus.y
                onTransformChanged()
                if (t >= 1f) break
            }
            animationJob = null
        }
    }

    internal fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    // Tiles

    private fun initializeTiles() {
        if (!canLayout() || source == null || tiles.isNotEmpty()) return
        fullImageSampleSize = calculateSampleSize(minScale).let { if (it > 1) it / 2 else it }

        // The view in the stored image's orientation
        val quarterTurn = orientation == 90 || orientation == 270
        val viewW = if (quarterTurn) viewHeight else viewWidth
        val viewH = if (quarterTurn) viewWidth else viewHeight
        val maxDetailTileWidth = max(MIN_TILE_SIZE, viewW / 2)
        val maxDetailTileHeight = max(MIN_TILE_SIZE, viewH / 2)

        val levels = mutableMapOf<Int, List<Tile>>()
        var sampleSize = fullImageSampleSize
        while (true) {
            val isBase = sampleSize == fullImageSampleSize
            val maxWidth = if (isBase) MAX_TILE_SIZE else min(MAX_TILE_SIZE, maxDetailTileWidth)
            val maxHeight = if (isBase) MAX_TILE_SIZE else min(MAX_TILE_SIZE, maxDetailTileHeight)
            var xTiles = 1
            var yTiles = 1
            while (rawWidth / xTiles / sampleSize > maxWidth) xTiles++
            while (rawHeight / yTiles / sampleSize > maxHeight) yTiles++
            val tileWidth = rawWidth / xTiles
            val tileHeight = rawHeight / yTiles
            val grid = ArrayList<Tile>(xTiles * yTiles)
            for (x in 0 until xTiles) {
                for (y in 0 until yTiles) {
                    grid.add(
                        Tile(
                            sampleSize,
                            Rect(
                                x * tileWidth,
                                y * tileHeight,
                                if (x == xTiles - 1) rawWidth else (x + 1) * tileWidth,
                                if (y == yTiles - 1) rawHeight else (y + 1) * tileHeight
                            )
                        )
                    )
                }
            }
            levels[sampleSize] = grid
            if (sampleSize == 1) break
            sampleSize /= 2
        }
        tiles = levels

        // The base layer is always kept
        levels[fullImageSampleSize]?.forEach { load(it) }
        refreshTiles()
    }

    /** Loads the visible tiles at the resolution the scale needs and lets go of the rest. */
    private fun refreshTiles() {
        if (tiles.isEmpty() || !hasTransform) return
        val sampleSize = min(fullImageSampleSize, calculateSampleSize(scale))
        currentSampleSize = sampleSize
        val visible = visibleRawRegion()
        for ((level, grid) in tiles) {
            if (level == fullImageSampleSize) continue
            for (tile in grid) {
                if (level == sampleSize && Rect.intersects(tile.rawRect, visible)) {
                    load(tile)
                } else {
                    tile.clear()
                }
            }
        }
    }

    private fun load(tile: Tile) {
        if (tile.bitmap != null || tile.job != null) return
        val source = source ?: return
        val scope = scope ?: return
        tile.job = scope.launch {
            val bitmap = try {
                source.decode(tile.rawRect, tile.sampleSize)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Unable to decode tile", e)
                null
            }
            tile.job = null
            if (this@SubsamplingImageState.source !== source) return@launch
            tile.bitmap = bitmap
            if (tile.sampleSize == fullImageSampleSize) {
                checkReady()
            }
            invalidate()
        }
    }

    private fun checkReady() {
        if (isReady) return
        val base = tiles[fullImageSampleSize] ?: return
        if (base.all { it.bitmap != null }) {
            isReady = true
            readyListeners.toList().forEach { it() }
        }
    }

    private fun calculateSampleSize(scale: Float): Int {
        val reqWidth = (width * scale).toInt()
        val reqHeight = (height * scale).toInt()
        if (reqWidth == 0 || reqHeight == 0) {
            return 32
        }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = (height / reqHeight.toFloat()).roundToInt()
            val widthRatio = (width / reqWidth.toFloat()).roundToInt()
            inSampleSize = min(heightRatio, widthRatio)
        }
        var power = 1
        while (power * 2 < inSampleSize) {
            power *= 2
        }
        return power
    }

    /** The part of the stored image the view shows, as a bounding box (the view may be rotated). */
    private fun visibleRawRegion(): Rect {
        val corners = floatArrayOf(
            0f, 0f,
            viewWidth.toFloat(), 0f,
            viewWidth.toFloat(), viewHeight.toFloat(),
            0f, viewHeight.toFloat()
        )
        buildMatrix(matrix)
        if (!matrix.invert(inverse)) {
            return Rect(0, 0, rawWidth, rawHeight)
        }
        inverse.mapPoints(corners)
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = -Float.MAX_VALUE
        var bottom = -Float.MAX_VALUE
        for (i in corners.indices step 2) {
            left = min(left, corners[i])
            right = max(right, corners[i])
            top = min(top, corners[i + 1])
            bottom = max(bottom, corners[i + 1])
        }
        return Rect(left.toInt() - 1, top.toInt() - 1, right.toInt() + 1, bottom.toInt() + 1)
    }

    /** Stored image pixels to view pixels: orientation, then scale and translation, then the view rotation. */
    private fun buildMatrix(target: Matrix) {
        when (orientation) {
            90 -> target.setValues(floatArrayOf(0f, -1f, rawHeight.toFloat(), 1f, 0f, 0f, 0f, 0f, 1f))
            180 -> target.setValues(
                floatArrayOf(-1f, 0f, rawWidth.toFloat(), 0f, -1f, rawHeight.toFloat(), 0f, 0f, 1f)
            )

            270 -> target.setValues(floatArrayOf(0f, 1f, 0f, -1f, 0f, rawWidth.toFloat(), 0f, 0f, 1f))
            else -> target.reset()
        }
        target.postScale(scale, scale)
        target.postTranslate(translateX, translateY)
        if (rotation != 0f) {
            target.postRotate(rotation, viewWidth / 2f, viewHeight / 2f)
        }
    }

    internal fun draw(canvas: Canvas) {
        if (!hasTransform || tiles.isEmpty()) return
        buildMatrix(matrix)
        canvas.save()
        canvas.concat(matrix)
        // The whole image underneath, then the sharper tiles that have arrived over it
        tiles[fullImageSampleSize]?.forEach { drawTile(canvas, it) }
        if (currentSampleSize != fullImageSampleSize) {
            tiles[currentSampleSize]?.forEach { drawTile(canvas, it) }
        }
        canvas.restore()
    }

    private fun drawTile(canvas: Canvas, tile: Tile) {
        val bitmap = tile.bitmap ?: return
        drawRect.set(tile.rawRect)
        canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint)
    }

    private class Tile(val sampleSize: Int, val rawRect: Rect) {
        var bitmap: Bitmap? = null
        var job: Job? = null

        fun clear() {
            job?.cancel()
            job = null
            // Not recycled: the last frame may still reference it, so the garbage collector frees it
            bitmap = null
        }
    }

    private fun easeOutQuad(t: Float): Float {
        return -t * (t - 2f)
    }

    private fun easeInOutQuad(t: Float): Float {
        val x = t * 2f
        return if (x < 1f) {
            0.5f * x * x
        } else {
            val y = x - 1f
            -0.5f * (y * (y - 2f) - 1f)
        }
    }

    companion object {
        private const val TAG = "SubsamplingImage"
        private const val DOUBLE_TAP_DURATION = 500L
        private const val FLING_DURATION = 500L
        private const val FLING_DISTANCE = 0.25f
        private const val MAX_TILE_SIZE = 2048
        private const val MIN_TILE_SIZE = 256
    }
}
