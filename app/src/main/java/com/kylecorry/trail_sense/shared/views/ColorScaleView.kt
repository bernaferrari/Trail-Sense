package com.kylecorry.trail_sense.shared.views

import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.scales.IColorScale

class ColorScaleState {
    var view: View? = null

    var colorScale by mutableStateOf<IColorScale?>(null)
    var labels by mutableStateOf<Map<Float, String>>(emptyMap())

    var isVisible: Boolean
        get() = view?.isVisible == true
        set(value) {
            view?.isVisible = value
        }
}

@Composable
fun ColorScaleContent(
    state: ColorScaleState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = Color(Resources.androidTextColorSecondary(context))
    val textSize = 12.sp

    Canvas(modifier = modifier.fillMaxSize()) {
        val colorScale = state.colorScale ?: return@Canvas
        val labelPadding = 2.5.dp.toPx()
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            this.textSize = textSize.toPx()
        }
        val labelHeight = paint.fontMetrics.run { bottom - top }
        val scaleHeight = size.height - labelHeight - labelPadding * 2

        for (i in 0..size.width.toInt()) {
            val pct = i / size.width
            drawLine(
                color = Color(colorScale.getColor(pct)),
                start = androidx.compose.ui.geometry.Offset(i.toFloat(), 0f),
                end = androidx.compose.ui.geometry.Offset(i.toFloat(), scaleHeight)
            )
        }

        drawIntoCanvas { canvas ->
            state.labels.forEach { label ->
                canvas.nativeCanvas.drawText(
                    label.value,
                    size.width * label.key,
                    size.height - labelPadding,
                    paint
                )
            }
        }
    }
}
