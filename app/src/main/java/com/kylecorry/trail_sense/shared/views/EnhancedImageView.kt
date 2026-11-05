package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Path
import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.subscaleview.ImageTileSourceSpec
import com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingImage
import com.kylecorry.trail_sense.shared.views.subscaleview.SubsamplingImageState
import kotlin.math.max

/**
 * A large image (or PDF page) in tiles with pan and zoom, and an overlay drawn in image coordinates over it.
 * A rendering object: keep it in the composition (`remember`) and draw it with [Content].
 */
open class EnhancedImageView(
    protected val context: Context
) {

    fun post(action: () -> Unit) {
        action()
    }

    private var backgroundColorOverride: Int? = null
    protected lateinit var drawer: ICanvasDrawer
    private var isSetup = false
    private var renderVersion by mutableIntStateOf(0)

    private val image = SubsamplingImageState(context).apply {
        maxScale = 6f
        doubleTapZoomBy = 2f
        addOnReadyListener { this@EnhancedImageView.onImageLoaded() }
    }

    protected var imageRotation = 0f
        set(value) {
            field = value
            updateRotation()
            invalidate()
        }

    private val imageClipPath = Path()
    private val files = FileSubsystem.getInstance(context)
    private var lastImage: String? = null
    private var lastScale = 1f
    private var lastTranslateX = 0f
    private var lastTranslateY = 0f
    private var rotationOffset = 0f

    protected val imageSize: Size
        get() = Size(imageWidth.toFloat(), imageHeight.toFloat())

    val orientation: Int
        get() = image.orientation

    val scale: Float
        get() = image.scale

    protected var minScale: Float
        get() = image.minScale
        set(value) {
            image.minScale = value
        }

    protected var maxScale: Float
        get() = image.maxScale
        set(value) {
            image.maxScale = value
        }

    protected val center: PointF?
        get() = image.getCenter()

    protected val isImageLoaded: Boolean
        get() = image.isReady

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        TrailSenseTheme {
            SubsamplingImage(
                state = image,
                modifier = modifier,
                background = backgroundColorOverride?.let { Color(it) } ?: Color.Transparent,
                onTap = { onSinglePress(it) }
            ) {
                renderVersion
                drawIntoCanvas {
                    val canvas = it.nativeCanvas
                    if (::drawer.isInitialized) {
                        drawer.canvas = canvas
                    } else {
                        drawer = CanvasDrawer(context, canvas)
                    }
                    renderOverlay()
                }
            }
        }
    }

    fun invalidate() {
        renderVersion++
    }

    fun setBackgroundColor(color: Int) {
        backgroundColorOverride = color
        invalidate()
    }

    val width: Int
        get() = image.viewWidth

    val height: Int
        get() = image.viewHeight

    private fun renderOverlay() {
        if (!image.isReady) {
            return
        }

        if (!isSetup) {
            setup()
            isSetup = true
        }

        drawer.push()
        drawer.rotate(viewRotation, width / 2f, height / 2f)
        myDraw()
        tryOrNothing {
            drawer.pop()
        }

        drawOverlay()
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

        if (image.translateX != lastTranslateX || image.translateY != lastTranslateY) {
            onTranslateChanged(lastTranslateX, lastTranslateY, image.translateX, image.translateY)
            lastTranslateX = image.translateX
            lastTranslateY = image.translateY
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
        rotationOffset = Trigonometry.deltaAngle(
            baseRotation.toFloat(),
            rotation
        )
        image.orientation = when (baseRotation) {
            90, 180, 270 -> baseRotation
            else -> 0
        }
        updateRotation()

        // Leave room for the image to turn by its offset from the quarter turn
        val offset = rotationOffset
        image.minScaleFactor = { width, height ->
            val rotatedSize = Size(width, height).rotate(offset)
            1f / max(rotatedSize.width / width, rotatedSize.height / height)
        }

        if (lastImage != filename) {
            image.setSource(
                ImageTileSourceSpec(
                    files.uri(filename),
                    isPdf = filename.lowercase().endsWith(".pdf")
                )
            )
            lastImage = filename
        }
        invalidate()
    }

    /** The image and overlay turn together by how far the image is rotated from its own offset. */
    private val viewRotation: Float
        get() = -imageRotation + rotationOffset

    private fun updateRotation() {
        image.rotation = viewRotation
    }

    private fun getBaseRotation(rotation: Float): Int {
        return rotation.roundNearestAngle(90f).toInt()
    }

    protected open fun onImageLoaded() {
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
        image.resetScaleAndCenter()
    }

    fun moveTo(x: Float, y: Float) {
        image.setScaleAndCenter(scale, PointF(x, y))
    }

    fun zoomBy(multiple: Float) {
        requestScale((scale * multiple).coerceIn(minScale, max(2 * minScale, maxScale)))
    }

    protected fun requestScale(scale: Float) {
        image.requestScale(scale)
    }

    protected fun requestCenter(point: PointF) {
        image.requestCenter(point)
    }

    protected fun toView(
        sourceX: Float,
        sourceY: Float,
        withRotation: Boolean = false
    ): PointF? {
        val view = image.sourceToView(sourceX, sourceY) ?: return null
        if (!withRotation) {
            return view
        }
        return rotate(view, viewRotation)
    }

    protected fun toSource(
        viewX: Float,
        viewY: Float,
        withRotation: Boolean = false
    ): PointF? {
        val view = if (withRotation) {
            rotate(PointF(viewX, viewY), -viewRotation)
        } else {
            PointF(viewX, viewY)
        }
        return image.viewToSource(view.x, view.y)
    }

    private fun rotate(point: PointF, degrees: Float): PointF {
        if (degrees == 0f) return point
        val radians = Math.toRadians(degrees.toDouble())
        val cos = kotlin.math.cos(radians).toFloat()
        val sin = kotlin.math.sin(radians).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val dx = point.x - cx
        val dy = point.y - cy
        return PointF(cx + dx * cos - dy * sin, cy + dx * sin + dy * cos)
    }

    val imageWidth: Int
        get() = image.width

    val imageHeight: Int
        get() = image.height

    protected open fun onSinglePress(position: Offset) {
        // Do nothing
    }
}
