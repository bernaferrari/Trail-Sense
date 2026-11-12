package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint

/**
 * Single-line map text in the platform's default typeface: measured by its tight glyph bounds and drawn on its
 * baseline, the way map labels have always been placed.
 */
internal expect class PlatformText() {
    /** The tight bounds of [text] at [size] pixels, as (width, height). */
    fun measure(text: String, size: Float): Pair<Float, Float>

    /** Draws [text] with its baseline starting at ([x], [y]) in [paint]'s color, style and filters. */
    fun draw(canvas: Canvas, text: String, x: Float, y: Float, size: Float, paint: Paint)
}
