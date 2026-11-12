package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText

/**
 * Draws [text] centered on [centerX] with its first baseline on [baseline], the way a canvas
 * text call with center alignment places it.
 */
fun DrawScope.drawCenteredText(
    measurer: TextMeasurer,
    text: String,
    centerX: Float,
    baseline: Float,
    style: TextStyle
) {
    val layout = measurer.measure(text, style)
    drawText(
        layout,
        topLeft = Offset(centerX - layout.size.width / 2f, baseline - layout.firstBaseline)
    )
}
