package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StampedPathEffectStyle

/**
 * Line patterns for paths on the map. The stamped ones repeat a small shape along the line, turned to follow it,
 * and are drawn with a fill.
 */
object MapPathEffects {

    fun dashed(dash: Float, gap: Float, phase: Float = 0f): PathEffect {
        return PathEffect.dashPathEffect(floatArrayOf(dash, gap), phase)
    }

    /** Dots of radius [size], [advance] apart. */
    fun dotted(size: Float = 3f, advance: Float = 10f, phase: Float = 0f): PathEffect {
        return stamp(Path().apply { addOval(Rect(Offset.Zero, size)) }, advance, phase)
    }

    /** Arrowheads pointing along the line, [size] from the center to each corner. */
    fun arrow(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f): PathEffect {
        return stamp(Path().apply {
            moveTo(0f, -size)
            lineTo(size, 0f)
            lineTo(0f, size)
            close()
        }, advance, phase)
    }

    fun square(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f): PathEffect {
        return stamp(Path().apply { addRect(Rect(-size / 2f, -size / 2f, size / 2f, size / 2f)) }, advance, phase)
    }

    fun diamond(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f): PathEffect {
        return stamp(Path().apply {
            moveTo(0f, -size / 2)
            lineTo(size / 2, 0f)
            lineTo(0f, size / 2)
            lineTo(-size / 2, 0f)
            close()
        }, advance, phase)
    }

    /** An X made of two bars, [size] across. */
    fun cross(size: Float = 6f, advance: Float = 3 * size, phase: Float = 0f): PathEffect {
        val thickness = size / 8
        val offset = thickness * 0.70710677f
        return stamp(Path().apply {
            moveTo(-size / 2 + offset, -size / 2 - offset)
            lineTo(-size / 2 - offset, -size / 2 + offset)
            lineTo(size / 2 - offset, size / 2 + offset)
            lineTo(size / 2 + offset, size / 2 - offset)
            close()
            moveTo(-size / 2 - offset, size / 2 - offset)
            lineTo(-size / 2 + offset, size / 2 + offset)
            lineTo(size / 2 + offset, -size / 2 + offset)
            lineTo(size / 2 - offset, -size / 2 - offset)
            close()
        }, advance, phase)
    }

    private fun stamp(shape: Path, advance: Float, phase: Float): PathEffect {
        return PathEffect.stampedPathEffect(shape, advance, phase, StampedPathEffectStyle.Rotate)
    }
}
