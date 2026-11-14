package com.kylecorry.andromeda.canvas

import com.kylecorry.andromeda.canvas.ArcMode
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.canvas.TextAlign
import com.kylecorry.andromeda.canvas.TextMode
import com.kylecorry.andromeda.canvas.TextStyle

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.PathEffect
import android.graphics.Shader
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.createBitmap

public interface ICanvasDrawer {
    public var canvas: Canvas
    public fun background(@ColorInt color: Int)
    public fun clear()

    @ColorInt
    public fun color(r: Int, g: Int = r, b: Int = g, a: Int? = null): Int
    public fun fill(@ColorInt color: Int)
    public fun tint(@ColorInt color: Int)
    public fun noTint()
    public fun stroke(@ColorInt color: Int)
    public fun pathEffect(effect: PathEffect)
    public fun noPathEffect()
    public fun noStroke()
    public fun noFill()
    public fun strokeWeight(pixels: Float)
    public fun strokeCap(cap: StrokeCap)
    public fun strokeJoin(join: StrokeJoin)
    public fun opacity(value: Int)
    public fun erase()
    public fun noErase()
    public fun smooth()
    public fun noSmooth()
    public fun shader(shader: Shader?)

    // TEXT HELPERS
    public fun textAlign(align: TextAlign)
    public fun textSize(pixels: Float)
    public fun textStyle(style: TextStyle)
    public fun textWidth(text: String): Float
    public fun textHeight(text: String): Float
    public fun textDimensions(text: String): Pair<Float, Float>
    public fun pathWidth(path: Path): Float
    public fun pathHeight(path: Path): Float
    public fun pathDimensions(path: Path): Pair<Float, Float>
    public fun textAscent(): Float
    public fun textDescent(): Float
    public fun text(str: String, x: Float, y: Float)

    // SHAPE HELPERS
    public fun arc(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        start: Float,
        stop: Float,
        mode: ArcMode = ArcMode.Pie
    )

    public fun ellipse(x: Float, y: Float, w: Float, h: Float = w)
    public fun circle(x: Float, y: Float, diameter: Float)
    public fun line(x1: Float, y1: Float, x2: Float, y2: Float)
    public fun lines(points: FloatArray)
    public fun grid(
        spacing: Float,
        width: Float = canvas.width.toFloat(),
        height: Float = canvas.height.toFloat()
    )

    public fun point(x: Float, y: Float)

    // TODO: Support different radius for each corner
    public fun rect(
        x: Float,
        y: Float,
        w: Float,
        h: Float = w,
        radius: Float = 0f
    )

    public fun square(
        x: Float,
        y: Float,
        size: Float,
        radius: Float = 0f
    )

    public fun path(value: Path)
    public fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)

    // Transforms
    // TODO: Add the transforms
    public fun push()
    public fun pop()
    public fun rotate(
        degrees: Float,
        originX: Float = canvas.width / 2f,
        originY: Float = canvas.height / 2f
    )

    public fun scale(x: Float, y: Float = x)
    public fun scale(x: Float, y: Float = x, pivotX: Float, pivotY: Float)
    public fun translate(x: Float, y: Float)
    public fun loadImage(@DrawableRes id: Int, w: Int? = null, h: Int? = null): Bitmap
    public fun image(
        img: Bitmap,
        x: Float,
        y: Float,
        w: Float = img.width.toFloat(),
        h: Float = img.height.toFloat()
    )

    public fun image(
        img: Bitmap,
        dx: Float,
        dy: Float,
        dw: Float,
        dh: Float,
        sx: Float,
        sy: Float,
        sw: Float = img.width.toFloat(),
        sh: Float = img.height.toFloat()
    )

    public fun imageMode(imageMode: ImageMode)
    public fun textMode(textMode: TextMode)
    public fun clip(path: Path)
    public fun clipInverse(path: Path)
    public fun mask(
        mask: Bitmap,
        tempBitmap: Bitmap = createBitmap(mask.width, mask.height),
        block: () -> Unit
    ): Bitmap

    public fun dp(size: Float): Float
    public fun sp(size: Float): Float
}