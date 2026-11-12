package com.kylecorry.trail_sense.shared.canvas

import android.graphics.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas

internal actual class PlatformText actual constructor() {
    private val measurePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val bounds = Rect()

    actual fun measure(text: String, size: Float): Pair<Float, Float> {
        measurePaint.textSize = size
        measurePaint.getTextBounds(text, 0, text.length, bounds)
        return bounds.width().toFloat() to bounds.height().toFloat()
    }

    actual fun draw(canvas: Canvas, text: String, x: Float, y: Float, size: Float, paint: Paint) {
        val framework = paint.asFrameworkPaint()
        framework.textSize = size
        canvas.nativeCanvas.drawText(text, x, y, framework)
    }
}
