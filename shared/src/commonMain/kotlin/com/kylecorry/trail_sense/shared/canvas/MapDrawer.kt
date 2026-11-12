package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.sol.math.geometry.Rectangle

/**
 * An immediate-mode drawing surface for the map and its layers, in the style of Processing: set the fill, stroke,
 * text and image state, then draw shapes with it. Colors are ARGB ints. Coordinates are pixels.
 */
interface MapDrawer {
    /** The surface's size in pixels. */
    val width: Float
    val height: Float

    // Paint state
    fun background(color: Int)
    fun clear()
    fun fill(color: Int)
    fun noFill()
    fun stroke(color: Int)
    fun noStroke()
    fun strokeWeight(pixels: Float)
    fun strokeCap(cap: StrokeCap)
    fun strokeJoin(join: StrokeJoin)
    fun pathEffect(effect: PathEffect)
    fun noPathEffect()

    /** The alpha (0 to 255) of what is drawn next. */
    fun opacity(value: Int)

    /** Draws images (and shapes) in this color, keeping their alpha. */
    fun tint(color: Int)
    fun noTint()

    /** Anti-aliases the edges of shapes and images (the default). */
    fun smooth()

    /** Draws hard edges, so adjoining images (such as map tiles) leave no seams between them. */
    fun noSmooth()

    // Text
    fun textSize(pixels: Float)
    fun textMode(mode: TextMode)

    /** The tight bounds of [text] at the current text size, as (width, height). */
    fun textDimensions(text: String): Pair<Float, Float>
    fun textWidth(text: String): Float = textDimensions(text).first
    fun textHeight(text: String): Float = textDimensions(text).second
    fun text(str: String, x: Float, y: Float)

    // Shapes
    fun circle(x: Float, y: Float, diameter: Float)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float)

    /** Draws a line for each 4 values (x1, y1, x2, y2) in [points]. */
    fun lines(points: FloatArray)
    fun rect(x: Float, y: Float, w: Float, h: Float = w, radius: Float = 0f)
    fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)
    fun path(value: Path)
    fun pathDimensions(path: Path): Pair<Float, Float> {
        val bounds = path.getBounds()
        return bounds.width to bounds.height
    }

    fun pathWidth(path: Path): Float = pathDimensions(path).first

    // Images
    fun imageMode(mode: ImageMode)
    fun image(img: ImageBitmap, x: Float, y: Float, w: Float = img.width.toFloat(), h: Float = img.height.toFloat())

    /** Draws the [srcLeft], [srcTop], [srcWidth], [srcHeight] part of [img] into the destination rectangle. */
    fun image(
        img: ImageBitmap,
        srcLeft: Int,
        srcTop: Int,
        srcWidth: Int,
        srcHeight: Int,
        dstLeft: Float,
        dstTop: Float,
        dstWidth: Float,
        dstHeight: Float
    )

    // Transforms and clipping
    fun push()
    fun pop()
    fun rotate(degrees: Float, originX: Float = width / 2f, originY: Float = height / 2f)
    fun scale(x: Float, y: Float = x)
    fun scale(x: Float, y: Float, pivotX: Float, pivotY: Float)
    fun translate(x: Float, y: Float)
    fun clip(path: Path)

    /** Runs [block] with [matrix] applied on top of the current transform. */
    fun withTransform(matrix: Matrix, block: () -> Unit)

    /**
     * Runs [block] in an offscreen layer that is composited with [alpha] (0 to 255), optionally multiplying it onto
     * what is below instead of drawing over it.
     */
    fun withLayer(alpha: Int, multiply: Boolean = false, block: () -> Unit)

    // Units
    fun dp(size: Float): Float
    fun sp(size: Float): Float
}

/** The drawing surface as a rectangle (top is the larger y), optionally rotated about its center. */
fun MapDrawer.getBounds(rotation: Float = 0f): Rectangle {
    val rectangle = Rectangle(0f, height, width, 0f)
    return if (rotation != 0f) rectangle.rotate(rotation) else rectangle
}

/** Draws [block] at [opacity] (0 to 255), in a layer when it is translucent so overlapping shapes don't stack. */
inline fun MapDrawer.withLayerOpacity(opacity: Int, crossinline block: () -> Unit) {
    if (opacity == 255) {
        opacity(opacity)
        block()
        return
    }
    withLayer(opacity) { block() }
}

/** Multi-line text: each line below the last, [lineSpacing] apart. */
fun MapDrawer.text(str: String, x: Float, y: Float, lineSpacing: Float) {
    var lastHeight = 0f
    var lastY = y
    str.split("\n").forEachIndexed { index, line ->
        val newY = lastY + lastHeight + if (index == 0) 0f else lineSpacing
        lastY = newY
        lastHeight = textHeight(line)
        text(line, x, newY)
    }
}

fun MapDrawer.textDimensions(str: String, lineSpacing: Float): Pair<Float, Float> {
    val lines = str.split("\n")
    val totalTextHeight = lines.sumOf { textHeight(it).toDouble() }.toFloat()
    val maxWidth = lines.maxOfOrNull { textWidth(it) } ?: 0f
    return maxWidth to totalTextHeight + lineSpacing * (lines.size - 1)
}
