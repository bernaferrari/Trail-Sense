package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.scales.IColorScale

class ColorScaleState {
    var colorScale by mutableStateOf<IColorScale?>(null)
    var labels by mutableStateOf<Map<Float, String>>(emptyMap())
    var isVisible by mutableStateOf(false)
}

@Composable
fun ColorScaleContent(state: ColorScaleState, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    Canvas(modifier = modifier.fillMaxSize()) {
        val colorScale = state.colorScale ?: return@Canvas
        val labelPadding = 2.5.dp.toPx()
        val labelHeight = textMeasurer.measure("Ag", textStyle).size.height
        val scaleHeight = size.height - labelHeight - labelPadding * 2

        for (i in 0..size.width.toInt()) {
            val pct = i / size.width
            drawLine(
                color = Color(colorScale.getColor(pct)),
                start = Offset(i.toFloat(), 0f),
                end = Offset(i.toFloat(), scaleHeight)
            )
        }

        state.labels.forEach { (position, text) ->
            val label = textMeasurer.measure(text, textStyle)
            drawText(
                label,
                topLeft = Offset(
                    size.width * position - label.size.width / 2f,
                    size.height - labelPadding - label.size.height
                )
            )
        }
    }
}
