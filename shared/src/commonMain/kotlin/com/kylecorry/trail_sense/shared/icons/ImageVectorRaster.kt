package com.kylecorry.trail_sense.shared.icons

import androidx.collection.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath

/*
 * Trail Sense's icons are Compose vectors. Canvas drawing (the compass and map markers) and platform icons need them
 * as images, so they are drawn here, onto Compose's portable canvas, outside of composition.
 */

private val cache = LruCache<String, ImageBitmap>(256)

/** The vector as a [width]×[height] px image; tinted with [tint] (an ARGB color, on every painted pixel) when given. */
fun ImageVector.toImageBitmap(width: Int, height: Int = width, tint: Int? = null): ImageBitmap {
    val key = "$name:${hashCode()}:$width:$height:$tint"
    cache[key]?.let { return it }
    val image = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
    Canvas(image).apply {
        scale(image.width / viewportWidth, image.height / viewportHeight)
        draw(root, this)
    }
    val result = if (tint == null) image else {
        val tinted = ImageBitmap(image.width, image.height)
        Canvas(tinted).drawImage(image, Offset.Zero, Paint().apply {
            colorFilter = ColorFilter.tint(Color(tint), BlendMode.SrcIn)
        })
        tinted
    }
    cache.put(key, result)
    return result
}

private fun draw(node: VectorNode, canvas: Canvas) {
    when (node) {
        is VectorGroup -> {
            canvas.save()
            canvas.concat(Matrix().apply {
                // translate(t + p) · rotate · scale · translate(-p), as Android's vector groups
                translate(node.translationX + node.pivotX, node.translationY + node.pivotY)
                rotateZ(node.rotation)
                scale(node.scaleX, node.scaleY)
                translate(-node.pivotX, -node.pivotY)
            })
            if (node.clipPathData.isNotEmpty()) {
                canvas.clipPath(PathParser().addPathNodes(node.clipPathData).toPath())
            }
            node.forEach { draw(it, canvas) }
            canvas.restore()
        }

        is VectorPath -> {
            val path = PathParser().addPathNodes(node.pathData).toPath()
            path.fillType = node.pathFillType
            (node.fill as? SolidColor)?.let { fill ->
                canvas.drawPath(path, Paint().apply {
                    isAntiAlias = true
                    style = PaintingStyle.Fill
                    color = fill.value.copy(alpha = fill.value.alpha * node.fillAlpha)
                })
            }
            (node.stroke as? SolidColor)?.let { stroke ->
                canvas.drawPath(path, Paint().apply {
                    isAntiAlias = true
                    style = PaintingStyle.Stroke
                    color = stroke.value.copy(alpha = stroke.value.alpha * node.strokeAlpha)
                    strokeWidth = node.strokeLineWidth
                    strokeMiterLimit = node.strokeLineMiter
                    strokeCap = node.strokeLineCap
                    strokeJoin = node.strokeLineJoin
                })
            }
        }
    }
}
