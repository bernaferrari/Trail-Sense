package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeToFit
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.ImageMagnifier
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentBounds
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PixelBounds
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.fixPerspective
import kotlin.math.max
import kotlin.math.min

@Composable
fun rememberPerspectiveCorrectionState(): PerspectiveCorrectionState {
    val context = LocalContext.current
    val state = androidx.compose.runtime.remember(context) {
        PerspectiveCorrectionState(context)
    }
    DisposableEffect(state) {
        onDispose {
            state.clearImage()
        }
    }
    return state
}

@Composable
fun PerspectiveCorrection(
    state: PerspectiveCorrectionState,
    modifier: Modifier = Modifier,
    imagePath: String? = state.imagePath,
    mapRotation: Float = state.mapRotation,
    isPreview: Boolean = state.isPreview,
    onLoad: (Bitmap) -> Unit = state.onLoad
) {
    LaunchedEffect(mapRotation) {
        state.mapRotation = mapRotation
    }

    LaunchedEffect(isPreview) {
        state.isPreview = isPreview
    }

    LaunchedEffect(imagePath) {
        if (imagePath == null) {
            state.clearImage()
        } else if (state.imagePath != imagePath) {
            state.setImage(imagePath)
        }
    }

    LaunchedEffect(onLoad) {
        state.onLoad = onLoad
    }

    val bitmap = state.image
    LaunchedEffect(bitmap) {
        if (bitmap != null && !state.calledOnLoad) {
            state.calledOnLoad = true
            state.onLoad(bitmap)
        }
    }

    TrailSenseTheme {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged(state::setSize)
                .pointerInteropFilter { state.onTouchEvent(it) }
        ) {
            state.draw(drawContext.canvas.nativeCanvas, size.width, size.height)
        }
    }
}

class PerspectiveCorrectionState(
    private val context: Context
) {

    internal var image by mutableStateOf<Bitmap?>(null)
        private set

    var imagePath: String? = null
        private set

    private var size = IntSize.Zero
    private var linesLoaded = false
    private var scale = 0.9f
    private var topLeft = PixelCoordinate(0f, 0f)
    private var topRight = PixelCoordinate(0f, 0f)
    private var bottomLeft = PixelCoordinate(0f, 0f)
    private var bottomRight = PixelCoordinate(0f, 0f)
    private var movingCorner: Corner? = null
    private var movingOffset = PixelCoordinate(0f, 0f)
    private var sourceMatrix = Matrix()
    private var renderVersion by mutableIntStateOf(0)

    @ColorInt
    private var primaryColor = Resources.getPrimaryColor(context)
    private var imageX = 0f
    private var imageY = 0f
    internal var calledOnLoad = false
    private val files = FileSubsystem.getInstance(context)
    private val outlinePath = Path()
    private val imageClipPath = Path()
    private val overlayPath = Path()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var mapRotation: Float = 0f
        set(value) {
            if (field == value) {
                return
            }
            field = value
            loadImageIfReady()
            refresh()
        }

    var onLoad: (Bitmap) -> Unit = {}
    var hasChanges = false

    var isPreview = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            refresh()
        }

    fun setSize(newSize: IntSize) {
        if (size == newSize) {
            return
        }
        size = newSize
        loadImageIfReady()
        refresh()
    }

    internal fun draw(canvas: Canvas, width: Float, height: Float) {
        renderVersion
        loadImageIfReady()
        val currentImage = image ?: return
        if (!linesLoaded) {
            resetLines()
        }

        imageX = (width - currentImage.width * scale) / 2f
        imageY = (height - currentImage.height * scale) / 2f

        canvas.save()
        canvas.rotate(mapRotation, width / 2f, height / 2f)
        canvas.translate(imageX, imageY)
        canvas.scale(scale, scale)
        if (isPreview) {
            drawPreviewCanvas(canvas, currentImage, width, height)
        } else {
            drawEditCanvas(canvas, currentImage)
            drawMagnify(canvas, currentImage)
        }
        canvas.restore()
    }

    private fun refresh() {
        renderVersion++
    }

    private fun loadImageIfReady() {
        if (image == null && imagePath != null && size.width > 0 && size.height > 0) {
            imagePath?.let { loadImage(it) }
        }
    }

    private fun loadImage(path: String) {
        val border = Resources.dp(context, 48f).toInt()
        val isPortraitRotation = mapRotation == 0f || mapRotation == 180f
        val w = (if (isPortraitRotation) size.width else size.height) - border
        val h = (if (isPortraitRotation) size.height else size.width) - border
        val bitmap = files.bitmap(path, w, h) ?: return
        val newImage = bitmap.resizeToFit(w, h)
        image = newImage
        imageClipPath.reset()
        imageClipPath.moveTo(0f, 0f)
        imageClipPath.lineTo(0f, newImage.height.toFloat())
        imageClipPath.lineTo(newImage.width.toFloat(), newImage.height.toFloat())
        imageClipPath.lineTo(newImage.width.toFloat(), 0f)
        imageClipPath.close()
        if (newImage != bitmap) {
            bitmap.recycle()
        }
        calledOnLoad = false
    }

    private fun drawMagnify(canvas: Canvas, image: Bitmap) {
        val corner = movingCorner ?: return
        val center = when (corner) {
            Corner.TopLeft -> topLeft
            Corner.TopRight -> topRight
            Corner.BottomLeft -> bottomLeft
            Corner.BottomRight -> bottomRight
        }

        val magnifierSize = min(image.width, image.height) / 4f
        val magnifier = ImageMagnifier(
            Size(image.width.toFloat(), image.height.toFloat()),
            Size(magnifierSize, magnifierSize)
        )
        val pos = magnifier.getMagnifierPosition(center)
        val magCenter = PixelCoordinate(pos.x + magnifierSize / 2f, pos.y + magnifierSize / 2f)
        val magnifierImage = magnifier.magnify(image, center)

        canvas.drawBitmap(
            magnifierImage,
            magCenter.x - magnifierImage.width / 2f,
            magCenter.y - magnifierImage.height / 2f,
            bitmapPaint
        )
        magnifierImage.recycle()

        overlayPaint.apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = Resources.dp(context, 2f)
        }
        val plusSize = Resources.dp(context, 8f)
        canvas.drawLine(
            magCenter.x - plusSize / 2f,
            magCenter.y,
            magCenter.x + plusSize / 2f,
            magCenter.y,
            overlayPaint
        )
        canvas.drawLine(
            magCenter.x,
            magCenter.y - plusSize / 2f,
            magCenter.x,
            magCenter.y + plusSize / 2f,
            overlayPaint
        )
    }

    private fun drawPreviewCanvas(canvas: Canvas, bitmap: Bitmap, viewWidth: Float, viewHeight: Float) {
        val warped = bitmap.fixPerspective(getBounds(), false, Color.WHITE)
        canvas.save()
        canvas.translate(-imageX, -imageY)
        val newImageX = (viewWidth - warped.width * scale) / 2f
        val newImageY = (viewHeight - warped.height * scale) / 2f
        canvas.translate(newImageX, newImageY)
        canvas.drawBitmap(warped, 0f, 0f, bitmapPaint)
        canvas.restore()
        if (warped != bitmap) {
            warped.recycle()
        }
    }

    private fun drawEditCanvas(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint)

        outlinePath.reset()
        outlinePath.moveTo(topLeft.x, topLeft.y)
        outlinePath.lineTo(topRight.x, topRight.y)
        outlinePath.lineTo(bottomRight.x, bottomRight.y)
        outlinePath.lineTo(bottomLeft.x, bottomLeft.y)
        outlinePath.close()

        overlayPaint.apply {
            color = Color.BLACK.withAlpha(128)
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.clipPath(imageClipPath)
        overlayPath.reset()
        overlayPath.addRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), Path.Direction.CW)
        overlayPath.op(outlinePath, Path.Op.DIFFERENCE)
        canvas.drawPath(overlayPath, overlayPaint)
        canvas.restore()

        overlayPaint.apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = Resources.dp(context, 2f)
        }
        canvas.drawPath(outlinePath, overlayPaint)

        overlayPaint.apply {
            color = primaryColor
            style = Paint.Style.FILL
        }
        val radius = Resources.dp(context, 10f)
        canvas.drawCircle(topLeft.x, topLeft.y, radius, overlayPaint)
        canvas.drawCircle(topRight.x, topRight.y, radius, overlayPaint)
        canvas.drawCircle(bottomLeft.x, bottomLeft.y, radius, overlayPaint)
        canvas.drawCircle(bottomRight.x, bottomRight.y, radius, overlayPaint)
    }

    private fun resetLines() {
        val currentImage = image ?: return
        topLeft = PixelCoordinate(0f, 0f)
        topRight = PixelCoordinate(currentImage.width.toFloat(), 0f)
        bottomLeft = PixelCoordinate(0f, currentImage.height.toFloat())
        bottomRight = PixelCoordinate(currentImage.width.toFloat(), currentImage.height.toFloat())
        linesLoaded = true
    }

    fun resetCorners() {
        val original = getBounds()
        resetLines()
        hasChanges = original != getBounds()
        refresh()
    }

    fun setBounds(bounds: PixelBounds) {
        val original = getBounds()
        topLeft = bounds.topLeft
        topRight = bounds.topRight
        bottomLeft = bounds.bottomLeft
        bottomRight = bounds.bottomRight
        hasChanges = original != bounds
        refresh()
    }

    private fun getBounds(): PixelBounds {
        return PixelBounds(topLeft, topRight, bottomLeft, bottomRight)
    }

    fun getPercentBounds(): PercentBounds? {
        val currentImage = image ?: return null
        return PercentBounds(
            PercentCoordinate(topLeft.x / currentImage.width, topLeft.y / currentImage.height),
            PercentCoordinate(topRight.x / currentImage.width, topRight.y / currentImage.height),
            PercentCoordinate(bottomLeft.x / currentImage.width, bottomLeft.y / currentImage.height),
            PercentCoordinate(bottomRight.x / currentImage.width, bottomRight.y / currentImage.height),
        )
    }

    fun setImage(path: String) {
        imagePath = path
        image = null
        linesLoaded = false
        calledOnLoad = false
        hasChanges = false
        loadImageIfReady()
        refresh()
    }

    fun clearImage() {
        imagePath = null
        val oldImage = image
        image = null
        oldImage?.recycle()
        linesLoaded = false
        calledOnLoad = false
        hasChanges = false
        refresh()
    }

    private fun toSource(pixel: PixelCoordinate): PixelCoordinate {
        sourceMatrix.reset()
        sourceMatrix.postRotate(mapRotation, size.width / 2f, size.height / 2f)
        sourceMatrix.invert(sourceMatrix)
        val point = floatArrayOf(pixel.x, pixel.y)
        sourceMatrix.mapPoints(point)
        val rotated = PixelCoordinate(point[0], point[1])
        return PixelCoordinate(
            rotated.x / scale - imageX / scale,
            rotated.y / scale - imageY / scale
        )
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val position = toSource(PixelCoordinate(event.x, event.y))
        val radius = Resources.dp(context, 16f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                when {
                    topLeft.distanceTo(position) <= radius -> {
                        movingCorner = Corner.TopLeft
                        movingOffset =
                            PixelCoordinate(position.x - topLeft.x, position.y - topLeft.y)
                    }

                    topRight.distanceTo(position) <= radius -> {
                        movingCorner = Corner.TopRight
                        movingOffset =
                            PixelCoordinate(position.x - topRight.x, position.y - topRight.y)
                    }

                    bottomLeft.distanceTo(position) <= radius -> {
                        movingCorner = Corner.BottomLeft
                        movingOffset =
                            PixelCoordinate(position.x - bottomLeft.x, position.y - bottomLeft.y)
                    }

                    bottomRight.distanceTo(position) <= radius -> {
                        movingCorner = Corner.BottomRight
                        movingOffset =
                            PixelCoordinate(position.x - bottomRight.x, position.y - bottomRight.y)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val actualPosition =
                    PixelCoordinate(position.x - movingOffset.x, position.y - movingOffset.y)
                when (movingCorner) {
                    Corner.TopLeft -> {
                        topLeft =
                            constrain(actualPosition, null, bottomLeft.y, null, topRight.x, radius)
                        hasChanges = true
                    }

                    Corner.TopRight -> {
                        topRight =
                            constrain(actualPosition, null, bottomRight.y, topLeft.x, null, radius)
                        hasChanges = true
                    }

                    Corner.BottomLeft -> {
                        bottomLeft =
                            constrain(actualPosition, topLeft.y, null, null, bottomRight.x, radius)
                        hasChanges = true
                    }

                    Corner.BottomRight -> {
                        bottomRight =
                            constrain(actualPosition, topRight.y, null, bottomLeft.x, null, radius)
                        hasChanges = true
                    }

                    null -> {
                        // Do nothing, not a valid corner
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                movingCorner = null
            }
        }
        refresh()
        return true
    }

    private fun constrain(
        pixel: PixelCoordinate,
        top: Float?,
        bottom: Float?,
        left: Float?,
        right: Float?,
        radius: Float
    ): PixelCoordinate {
        var x = pixel.x
        var y = pixel.y

        if (top != null && y < top) {
            y = top
        }

        if (bottom != null && y > bottom) {
            y = bottom
        }

        if (left != null && x < left) {
            x = left
        }

        if (right != null && x > right) {
            x = right
        }

        val start = toSource(PixelCoordinate(radius, radius))
        val end = toSource(PixelCoordinate(size.width.toFloat() - radius, size.height.toFloat() - radius))

        return PixelCoordinate(
            x.coerceIn(min(start.x, end.x), max(start.x, end.x)),
            y.coerceIn(min(start.y, end.y), max(start.y, end.y))
        )
    }

    private enum class Corner {
        TopLeft,
        TopRight,
        BottomLeft,
        BottomRight
    }
}
