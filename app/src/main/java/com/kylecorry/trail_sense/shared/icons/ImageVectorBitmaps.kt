package com.kylecorry.trail_sense.shared.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.roundToInt

/*
 * Trail Sense's icons are Compose vectors. Places that need a bitmap or a platform icon (canvas drawing on the
 * compass and maps, notifications, home screen widgets and shortcuts) draw the same vector here, straight onto an
 * Android canvas, so there is one icon system and no drawable resources.
 */

private val cache = object : android.util.LruCache<String, Bitmap>(256) {}

/** The vector as a bitmap, [width]×[height] px; tinted with [tint] (every painted pixel) when given. */
fun ImageVector.toBitmap(width: Int, height: Int = width, @ColorInt tint: Int? = null): Bitmap {
    val key = "$name:${hashCode()}:$width:$height:$tint"
    cache.get(key)?.let { return it }
    val bitmap = createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
    val canvas = Canvas(bitmap)
    canvas.scale(width / viewportWidth, height / viewportHeight)
    draw(root, canvas)
    if (tint != null) {
        val tinted = createBitmap(bitmap.width, bitmap.height)
        Canvas(tinted).drawBitmap(bitmap, 0f, 0f, Paint().apply {
            colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
        })
        bitmap.recycle()
        cache.put(key, tinted)
        return tinted
    }
    cache.put(key, bitmap)
    return bitmap
}

/** The vector at [sizeDp] for this screen's density. */
fun ImageVector.toBitmap(context: Context, sizeDp: Float = 24f, @ColorInt tint: Int? = null): Bitmap {
    val px = (sizeDp * context.resources.displayMetrics.density).roundToInt()
    val height = (px * viewportHeight / viewportWidth).roundToInt()
    return toBitmap(px, height, tint)
}

/** A platform icon (notifications, shortcuts) drawn from the vector. */
fun ImageVector.toIconCompat(context: Context, sizeDp: Float = 24f, @ColorInt tint: Int? = null): IconCompat =
    IconCompat.createWithBitmap(toBitmap(context, sizeDp, tint))

private fun draw(node: VectorNode, canvas: Canvas) {
    when (node) {
        is VectorGroup -> {
            canvas.save()
            val matrix = Matrix().apply {
                postTranslate(-node.pivotX, -node.pivotY)
                postScale(node.scaleX, node.scaleY)
                postRotate(node.rotation)
                postTranslate(node.translationX + node.pivotX, node.translationY + node.pivotY)
            }
            canvas.concat(matrix)
            if (node.clipPathData.isNotEmpty()) {
                canvas.clipPath(PathParser().addPathNodes(node.clipPathData).toPath().asAndroidPath())
            }
            node.forEach { draw(it, canvas) }
            canvas.restore()
        }

        is VectorPath -> {
            val path = PathParser().addPathNodes(node.pathData).toPath().asAndroidPath()
            path.fillType = if (node.pathFillType == PathFillType.EvenOdd) {
                android.graphics.Path.FillType.EVEN_ODD
            } else {
                android.graphics.Path.FillType.WINDING
            }
            (node.fill as? SolidColor)?.let { fill ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = fill.value.copy(alpha = fill.value.alpha * node.fillAlpha).toArgb()
                }
                canvas.drawPath(path, paint)
            }
            (node.stroke as? SolidColor)?.let { stroke ->
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    color = stroke.value.copy(alpha = stroke.value.alpha * node.strokeAlpha).toArgb()
                    strokeWidth = node.strokeLineWidth
                    strokeMiter = node.strokeLineMiter
                    strokeCap = when (node.strokeLineCap) {
                        StrokeCap.Round -> Paint.Cap.ROUND
                        StrokeCap.Square -> Paint.Cap.SQUARE
                        else -> Paint.Cap.BUTT
                    }
                    strokeJoin = when (node.strokeLineJoin) {
                        StrokeJoin.Round -> Paint.Join.ROUND
                        StrokeJoin.Bevel -> Paint.Join.BEVEL
                        else -> Paint.Join.MITER
                    }
                }
                canvas.drawPath(path, paint)
            }
        }
    }
}
