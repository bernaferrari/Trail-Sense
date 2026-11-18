package com.kylecorry.trail_sense.shared.canvas

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Typeface

internal actual class PlatformText actual constructor() {
    private val typeface: Typeface? = listOf("Roboto", "Arial", "sans-serif")
        .firstNotNullOfOrNull { FontMgr.default.matchFamilyStyle(it, FontStyle.NORMAL) }
        ?: FontMgr.default.legacyMakeTypeface("", FontStyle.NORMAL)
    private val font = Font(typeface)
    private val textPaint = org.jetbrains.skia.Paint().apply { isAntiAlias = true }

    actual fun measure(text: String, size: Float): Pair<Float, Float> {
        font.size = size
        val bounds = font.measureText(text)
        return bounds.width to bounds.height
    }

    actual fun draw(canvas: Canvas, text: String, x: Float, y: Float, size: Float, paint: Paint) {
        font.size = size
        textPaint.color = paint.color.toArgb()
        textPaint.mode = if (paint.style == PaintingStyle.Stroke) PaintMode.STROKE else PaintMode.FILL
        textPaint.strokeWidth = paint.strokeWidth
        canvas.nativeCanvas.drawString(text, x, y, font, textPaint)
    }
}
