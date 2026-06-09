package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.views.subscaleview.ImageSource
import com.kylecorry.andromeda.views.subscaleview.SubsamplingScaleImageView
import com.kylecorry.andromeda.views.subscaleview.decoder.SkiaImageDecoder
import com.kylecorry.andromeda.views.subscaleview.decoder.SkiaImageRegionDecoder
import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.shared.canvas.tiles.PdfImageDecoder
import com.kylecorry.trail_sense.shared.canvas.tiles.PdfImageRegionDecoder
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import kotlin.math.max

open class EnhancedImageView(
    context: Context
) : AbstractComposeView(context) {

    val view: View
        get() = this

    fun post(action: () -> Unit) {
        if (!isAttachedToWindow) {
            action()
            return
        }
        super.post {
            action()
        }
    }

    protected lateinit var drawer: ICanvasDrawer
    private var isSetup = false
    private var renderVersion by mutableIntStateOf(0)
    private var contentWidth by mutableIntStateOf(0)
    private var contentHeight by mutableIntStateOf(0)

    private val imageView = TiledImageView(context)

    private inner class TiledImageView(
        context: Context
    ) : SubsamplingScaleImageView(context, null) {
        override fun invalidate() {
            super.invalidate()
            requestRedraw()
        }

        override fun tileVisible(tile: Tile?): Boolean {
            if (imageRotation == 0f && rotationOffset == 0f) {
                return super.tileVisible(tile)
            }

            val rect = tile?.sRect ?: return false

            val topLeftSource = toSource(0f, 0f, true)
            val topRightSource = toSource(width.toFloat(), 0f, true)
            val bottomRightSource = toSource(width.toFloat(), height.toFloat(), true)
            val bottomLeftSource = toSource(0f, height.toFloat(), true)

            val sVisLeft = minOf(
                topLeftSource?.x ?: 0f,
                topRightSource?.x ?: 0f,
                bottomRightSource?.x ?: 0f,
                bottomLeftSource?.x ?: 0f
            )
            val sVisRight = maxOf(
                topLeftSource?.x ?: 0f,
                topRightSource?.x ?: 0f,
                bottomRightSource?.x ?: 0f,
                bottomLeftSource?.x ?: 0f
            )
            val sVisTop = minOf(
                topLeftSource?.y ?: 0f,
                topRightSource?.y ?: 0f,
                bottomRightSource?.y ?: 0f,
                bottomLeftSource?.y ?: 0f
            )
            val sVisBottom = maxOf(
                topLeftSource?.y ?: 0f,
                topRightSource?.y ?: 0f,
                bottomRightSource?.y ?: 0f,
                bottomLeftSource?.y ?: 0f
            )

            return sVisLeft > rect.right || rect.left > sVisRight || sVisTop > rect.bottom || rect.top <= sVisBottom
        }

        override fun onImageLoaded() {
            super.onImageLoaded()
            this@EnhancedImageView.onImageLoaded()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val consumed = gestureDetector.onTouchEvent(event)
            return consumed || super.onTouchEvent(event)
        }

        fun refreshTiles() {
            refreshRequiredTiles(true)
        }

        fun isInteractingWithZoom(): Boolean {
            return isZooming || isQuickScaling
        }
    }

    protected var imageRotation = 0f
        set(value) {
            field = value
            imageView.refreshTiles()
            invalidate()
        }

    private val imageClipPath = Path()
    private val lookupMatrix = Matrix()
    private val files = FileSubsystem.getInstance(context)
    private var lastImage: String? = null
    private var lastScale = 1f
    private var lastTranslateX = 0f
    private var lastTranslateY = 0f
    private var rotationOffset = 0f

    protected val imageSize: Size
        get() = Size(imageWidth.toFloat(), imageHeight.toFloat())

    val orientation: Int
        get() = imageView.orientation

    val scale: Float
        get() = imageView.scale

    protected var minScale: Float
        get() = imageView.minScale
        set(value) {
            imageView.minScale = value
        }

    protected var maxScale: Float
        get() = imageView.maxScale
        set(value) {
            imageView.maxScale = value
        }

    protected val center: PointF?
        get() = imageView.center

    protected val isImageLoaded: Boolean
        get() = imageView.isImageLoaded

    @Composable
    override fun Content() {
        EnhancedImageContent()
    }

    @Composable
    fun Content(modifier: Modifier) {
        EnhancedImageContent(modifier)
    }

    @Composable
    fun EnhancedImageContent(modifier: Modifier = Modifier) {
        TrailSenseTheme {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .pointerInteropFilter {
                        imageView.dispatchTouchEvent(it)
                    }
                    .onSizeChanged {
                        contentWidth = it.width
                        contentHeight = it.height
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderVersion
                    if (contentWidth > 0 && contentHeight > 0) {
                        imageView.measure(
                            View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(contentHeight, View.MeasureSpec.EXACTLY)
                        )
                        imageView.layout(0, 0, contentWidth, contentHeight)
                        imageView.draw(drawContext.canvas.nativeCanvas)
                    }
                    if (::drawer.isInitialized) {
                        drawer.canvas = drawContext.canvas.nativeCanvas
                    } else {
                        drawer = CanvasDrawer(context, drawContext.canvas.nativeCanvas)
                    }
                    renderOverlay(drawContext.canvas.nativeCanvas)
                }
            }
        }
    }

    override fun invalidate() {
        super.invalidate()
        requestRedraw()
    }

    private fun requestRedraw() {
        post {
            renderVersion++
        }
    }

    private fun renderOverlay(canvas: AndroidCanvas) {
        if (!imageView.isReady) {
            return
        }

        if (!isSetup) {
            mySetup()
            isSetup = true
        }

        drawer.canvas = canvas
        drawer.push()
        drawer.rotate(-imageRotation + rotationOffset)
        myDraw()
        tryOrNothing {
            drawer.pop()
        }

        drawOverlay()
    }

    private fun mySetup() {
        imageView.setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_OUTSIDE)
        imageView.maxScale = 6f
        imageView.alwaysZoomDoubleTap = true
        imageView.alwaysZoomDoubleTapZoomScale = 2f
        setup()
    }

    private fun myDraw() {
        imageClipPath.apply {
            rewind()
            val topLeft = toView(0f, 0f) ?: return@apply
            val bottomRight = toView(imageWidth.toFloat(), imageHeight.toFloat()) ?: return@apply
            addRect(
                topLeft.x,
                topLeft.y,
                bottomRight.x,
                bottomRight.y,
                Path.Direction.CW
            )
        }

        if (scale != lastScale) {
            onScaleChanged(lastScale, scale)
            lastScale = scale
        }

        imageView.vTranslate?.let {
            if (it.x != lastTranslateX || it.y != lastTranslateY) {
                onTranslateChanged(lastTranslateX, lastTranslateY, it.x, it.y)
                lastTranslateX = it.x
                lastTranslateY = it.y
            }
        }

        preDraw()

        drawer.push()
        drawer.clip(imageClipPath)
        draw()
        drawer.pop()

        postDraw()
    }

    protected open fun postDraw() {
        // Do nothing
    }

    protected open fun drawOverlay() {
        // Do nothing
    }

    protected open fun preDraw() {
        // Do nothing
    }

    protected open fun draw() {
        // Do nothing
    }

    protected open fun setup() {
        // Do nothing
    }

    fun setImage(filename: String, rotation: Float = 0f) {
        val baseRotation = getBaseRotation(rotation)
        if (imageView.orientation != baseRotation) {
            imageView.orientation = when (baseRotation) {
                90 -> SubsamplingScaleImageView.ORIENTATION_90
                180 -> SubsamplingScaleImageView.ORIENTATION_180
                270 -> SubsamplingScaleImageView.ORIENTATION_270
                else -> SubsamplingScaleImageView.ORIENTATION_0
            }
        }
        rotationOffset = Trigonometry.deltaAngle(
            baseRotation.toFloat(),
            rotation
        )

        if (lastImage != filename) {
            val uri = files.uri(filename)

            if (filename.lowercase().endsWith(".pdf")) {
                imageView.setBitmapDecoderClass(PdfImageDecoder::class.java)
                imageView.setRegionDecoderClass(PdfImageRegionDecoder::class.java)
            } else {
                imageView.setBitmapDecoderClass(SkiaImageDecoder::class.java)
                imageView.setRegionDecoderClass(SkiaImageRegionDecoder::class.java)
            }

            imageView.setImage(ImageSource.uri(uri))
            lastImage = filename
        }
        invalidate()
    }

    private fun getBaseRotation(rotation: Float): Int {
        return rotation.roundNearestAngle(90f).toInt()
    }

    protected open fun onImageLoaded() {
        val rotatedImageSize =
            Size(imageWidth.toFloat(), imageHeight.toFloat()).rotate(rotationOffset)
        val percentIncrease = max(
            rotatedImageSize.width / imageWidth,
            rotatedImageSize.height / imageHeight
        )
        imageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
        imageView.minScale /= percentIncrease
        invalidate()
    }

    protected open fun onScaleChanged(oldScale: Float, newScale: Float) {
        // Do nothing
    }

    protected open fun onTranslateChanged(
        oldTranslateX: Float,
        oldTranslateY: Float,
        newTranslateX: Float,
        newTranslateY: Float
    ) {
        // Do nothing
    }

    fun recenter() {
        imageView.resetScaleAndCenter()
    }

    fun moveTo(x: Float, y: Float) {
        imageView.setScaleAndCenter(scale, PointF(x, y))
    }

    fun zoomBy(multiple: Float) {
        requestScale((scale * multiple).coerceIn(minScale, max(2 * minScale, maxScale)))
    }

    protected fun requestScale(scale: Float) {
        imageView.requestScale(scale)
    }

    protected fun requestCenter(point: PointF) {
        imageView.requestCenter(point)
    }

    protected fun toView(
        sourceX: Float,
        sourceY: Float,
        withRotation: Boolean = false
    ): PointF? {
        val view = imageView.sourceToViewCoord(sourceX, sourceY) ?: return null

        if (withRotation) {
            transform(view, inPlace = true) {
                postRotate(-imageRotation + rotationOffset, width / 2f, height / 2f)
            }
        }

        return view
    }

    protected fun toSource(
        viewX: Float,
        viewY: Float,
        withRotation: Boolean = false
    ): PointF? {
        val view = PointF(viewX, viewY)

        if (withRotation) {
            transform(view, invert = true, inPlace = true) {
                postRotate(-imageRotation + rotationOffset, width / 2f, height / 2f)
            }
        }

        return imageView.viewToSourceCoord(view.x, view.y)
    }

    val imageWidth: Int
        get() {
            return if (imageView.orientation == 90 || imageView.orientation == 270) {
                imageView.sHeight
            } else {
                imageView.sWidth
            }
        }

    val imageHeight: Int
        get() {
            return if (imageView.orientation == 90 || imageView.orientation == 270) {
                imageView.sWidth
            } else {
                imageView.sHeight
            }
        }

    protected open fun onLongPress(e: MotionEvent) {
        // Do nothing
    }

    protected open fun onSinglePress(e: MotionEvent) {
        // Do nothing
    }

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            if (imageView.isInteractingWithZoom()) return

            this@EnhancedImageView.onLongPress(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            this@EnhancedImageView.onSinglePress(e)
            return super.onSingleTapConfirmed(e)
        }
    }

    private val gestureDetector = GestureDetector(context, gestureListener)

    private fun transform(
        point: PointF,
        invert: Boolean = false,
        inPlace: Boolean = false,
        actions: Matrix.() -> Unit
    ): PointF {
        synchronized(lookupMatrix) {
            lookupMatrix.reset()
            actions(lookupMatrix)
            if (invert) {
                lookupMatrix.invert(lookupMatrix)
            }
            val pointArray = floatArrayOf(point.x, point.y)
            lookupMatrix.mapPoints(pointArray)

            if (inPlace) {
                point.x = pointArray[0]
                point.y = pointArray[1]
                return point
            }

            return PointF(pointArray[0], pointArray[1])
        }
    }
}
