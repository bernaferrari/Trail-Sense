package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap as ComposeStrokeCap
import androidx.compose.ui.graphics.StrokeJoin as ComposeStrokeJoin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextMode

/**
 * A [MapDrawer] on a Compose [Canvas], so the same layer code draws on every platform. Shapes and images go through
 * Compose's portable canvas; text goes through [PlatformText], which measures and draws it the way each platform's
 * own text API does.
 *
 * Point it at a new canvas each frame with [setCanvas]; the paint state carries over between frames.
 */
class ComposeMapDrawer(
    private val density: Float,
    private val fontScale: Float = 1f
) : MapDrawer {

    private var canvas: Canvas? = null
    override var width: Float = 0f
        private set
    override var height: Float = 0f
        private set

    private val fillPaint = Paint().apply { style = PaintingStyle.Fill }
    private val strokePaint = Paint().apply { style = PaintingStyle.Stroke }
    private var shouldFill = true
    private var shouldStroke = false
    private var imageMode = ImageMode.Corner
    private var textMode = TextMode.Corner
    private var textSize = 12f
    private val text = PlatformText()
    private val layerPaint = Paint()

    init {
        fillPaint.isAntiAlias = true
        strokePaint.isAntiAlias = true
        strokeCap(StrokeCap.Round)
        strokeJoin(StrokeJoin.Miter)
        fillPaint.color = Color.Black
        strokePaint.color = Color.Black
    }

    /** Draws on [canvas], which is [width] by [height] pixels, until the next call. */
    fun setCanvas(canvas: Canvas, width: Float, height: Float) {
        this.canvas = canvas
        this.width = width
        this.height = height
    }

    private val c: Canvas
        get() = checkNotNull(canvas) { "Call setCanvas before drawing" }

    override fun background(color: Int) {
        // The whole surface, whatever the current transform (as Android's Canvas.drawColor)
        c.drawRect(-LARGE, -LARGE, LARGE, LARGE, Paint().apply { this.color = Color(color) })
    }

    override fun clear() {
        // Each frame starts on a cleared surface; like drawing transparent over it, there is nothing to do.
    }

    // As Android's Paint, which the map was built on: a color sets the alpha too, and opacity then replaces it.
    override fun fill(color: Int) {
        shouldFill = true
        fillPaint.color = Color(color)
    }

    override fun noFill() {
        shouldFill = false
    }

    override fun stroke(color: Int) {
        shouldStroke = true
        strokePaint.color = Color(color)
    }

    override fun noStroke() {
        shouldStroke = false
    }

    override fun strokeWeight(pixels: Float) {
        strokePaint.strokeWidth = pixels
    }

    override fun strokeCap(cap: StrokeCap) {
        strokePaint.strokeCap = when (cap) {
            StrokeCap.Round -> ComposeStrokeCap.Round
            StrokeCap.Square -> ComposeStrokeCap.Square
            StrokeCap.Project -> ComposeStrokeCap.Butt
        }
    }

    override fun strokeJoin(join: StrokeJoin) {
        strokePaint.strokeJoin = when (join) {
            StrokeJoin.Miter -> ComposeStrokeJoin.Miter
            StrokeJoin.Bevel -> ComposeStrokeJoin.Bevel
            StrokeJoin.Round -> ComposeStrokeJoin.Round
        }
    }

    override fun pathEffect(effect: PathEffect) {
        fillPaint.pathEffect = effect
        strokePaint.pathEffect = effect
    }

    override fun noPathEffect() {
        fillPaint.pathEffect = null
        strokePaint.pathEffect = null
    }

    override fun opacity(value: Int) {
        val alpha = value.coerceIn(0, 255) / 255f
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
    }

    override fun tint(color: Int) {
        val filter = ColorFilter.tint(Color(color), BlendMode.SrcIn)
        fillPaint.colorFilter = filter
        strokePaint.colorFilter = filter
    }

    override fun noTint() {
        fillPaint.colorFilter = null
        strokePaint.colorFilter = null
    }

    override fun smooth() {
        fillPaint.isAntiAlias = true
        strokePaint.isAntiAlias = true
    }

    override fun noSmooth() {
        fillPaint.isAntiAlias = false
        strokePaint.isAntiAlias = false
    }

    override fun textSize(pixels: Float) {
        textSize = pixels
    }

    override fun textMode(mode: TextMode) {
        textMode = mode
    }

    override fun textDimensions(text: String): Pair<Float, Float> {
        return this.text.measure(text, textSize)
    }

    override fun text(str: String, x: Float, y: Float) {
        if (!shouldFill && !shouldStroke) return
        val (w, h) = if (textMode == TextMode.Center) textDimensions(str) else (0f to 0f)
        val realX = if (textMode == TextMode.Center) x - w / 2f else x
        val realY = if (textMode == TextMode.Center) y + h / 2f else y
        if (shouldStroke) text.draw(c, str, realX, realY, textSize, strokePaint)
        if (shouldFill) text.draw(c, str, realX, realY, textSize, fillPaint)
    }

    override fun circle(x: Float, y: Float, diameter: Float) {
        val radius = diameter / 2f
        if (shouldFill) c.drawCircle(Offset(x, y), radius, fillPaint)
        if (shouldStroke) c.drawCircle(Offset(x, y), radius, strokePaint)
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        if (shouldFill) c.drawLine(Offset(x1, y1), Offset(x2, y2), fillPaint)
        if (shouldStroke) c.drawLine(Offset(x1, y1), Offset(x2, y2), strokePaint)
    }

    override fun lines(points: FloatArray) {
        if (shouldFill) c.drawRawPoints(PointMode.Lines, points, fillPaint)
        if (shouldStroke) c.drawRawPoints(PointMode.Lines, points, strokePaint)
    }

    override fun rect(x: Float, y: Float, w: Float, h: Float, radius: Float) {
        fun draw(paint: Paint) {
            if (radius == 0f) {
                c.drawRect(x, y, x + w, y + h, paint)
            } else {
                c.drawRoundRect(x, y, x + w, y + h, radius, radius, paint)
            }
        }
        if (shouldFill) draw(fillPaint)
        if (shouldStroke) draw(strokePaint)
    }

    override fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        if (!shouldFill && !shouldStroke) return
        path(Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
            lineTo(x3, y3)
            close()
        })
    }

    override fun path(value: Path) {
        if (shouldFill) c.drawPath(value, fillPaint)
        if (shouldStroke) c.drawPath(value, strokePaint)
    }

    override fun imageMode(mode: ImageMode) {
        imageMode = mode
    }

    override fun image(img: ImageBitmap, x: Float, y: Float, w: Float, h: Float) {
        val left = if (imageMode == ImageMode.Center) x - w / 2f else x
        val top = if (imageMode == ImageMode.Center) y - h / 2f else y
        image(img, 0, 0, img.width, img.height, left, top, w, h)
    }

    override fun image(
        img: ImageBitmap,
        srcLeft: Int,
        srcTop: Int,
        srcWidth: Int,
        srcHeight: Int,
        dstLeft: Float,
        dstTop: Float,
        dstWidth: Float,
        dstHeight: Float
    ) {
        // Pixel-aligned like Android's Canvas.drawBitmap(Rect, Rect) the map was built on
        val left = dstLeft.toInt()
        val top = dstTop.toInt()
        c.drawImageRect(
            img,
            IntOffset(srcLeft, srcTop),
            IntSize(srcWidth, srcHeight),
            IntOffset(left, top),
            IntSize((dstLeft + dstWidth).toInt() - left, (dstTop + dstHeight).toInt() - top),
            fillPaint
        )
    }

    override fun push() {
        c.save()
    }

    override fun pop() {
        c.restore()
    }

    override fun rotate(degrees: Float, originX: Float, originY: Float) {
        c.translate(originX, originY)
        c.rotate(degrees)
        c.translate(-originX, -originY)
    }

    override fun scale(x: Float, y: Float) {
        c.scale(x, y)
    }

    override fun scale(x: Float, y: Float, pivotX: Float, pivotY: Float) {
        c.translate(pivotX, pivotY)
        c.scale(x, y)
        c.translate(-pivotX, -pivotY)
    }

    override fun translate(x: Float, y: Float) {
        c.translate(x, y)
    }

    override fun clip(path: Path) {
        c.clipPath(path)
    }

    override fun withTransform(matrix: Matrix, block: () -> Unit) {
        c.save()
        try {
            c.concat(matrix)
            block()
        } finally {
            c.restore()
        }
    }

    override fun withLayer(alpha: Int, multiply: Boolean, block: () -> Unit) {
        layerPaint.alpha = alpha.coerceIn(0, 255) / 255f
        layerPaint.blendMode = if (multiply) BlendMode.Multiply else BlendMode.SrcOver
        c.saveLayer(Rect(-LARGE, -LARGE, LARGE, LARGE), layerPaint)
        try {
            block()
        } finally {
            c.restore()
        }
    }

    override fun dp(size: Float): Float = size * density

    override fun sp(size: Float): Float = size * density * fontScale

    private companion object {
        // Large enough to cover any screen under any map transform, small enough to stay exact in floats
        const val LARGE = 1_000_000f
    }
}
