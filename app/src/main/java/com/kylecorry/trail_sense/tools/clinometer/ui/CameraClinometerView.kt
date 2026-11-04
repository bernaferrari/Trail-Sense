package com.kylecorry.trail_sense.tools.clinometer.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.absoluteValue
import kotlin.math.min

class CameraClinometerView {

    var inclination by mutableStateOf(0f)
    var startInclination by mutableStateOf<Float?>(null)
    private var cameraVisible by mutableStateOf(true)

    var isVisible: Boolean
        get() = cameraVisible
        set(value) {
            cameraVisible = value
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!cameraVisible) {
            return
        }
        CameraClinometerContent(
            inclination = inclination,
            startInclination = startInclination,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CameraClinometerContent(
    inclination: Float,
    startInclination: Float?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = FormatService.getInstance(context)
    val dialColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
    val ink = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
    val needle = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val status = com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors()
    val danger = androidx.compose.material3.MaterialTheme.colorScheme.error
    val density = LocalDensity.current
    val tickLength = with(density) { 4.dp.toPx() }
    val tickStroke = with(density) { 2.dp.toPx() }
    val needleStroke = with(density) { 4.dp.toPx() }
    val textSize = with(density) { 10.sp.toPx() }
    val tickInterval = 10
    val labelInterval = 30

    Canvas(modifier = modifier) {
        fun getY(incline: Float): Float {
            val padding = 20f
            val height = size.height - 2 * padding
            return height - (map(incline, -90f, 90f, 0f, 1f) * height) + padding
        }

        drawRect(dialColor)

        fun zone(start: Float, end: Float, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(0f, min(getY(start), getY(end))),
                size = Size(tickLength, (getY(start) - getY(end)).absoluteValue)
            )
        }

        zone(45f, 30f, danger)
        zone(-30f, -45f, danger)
        zone(60f, 45f, status.caution)
        zone(-45f, -60f, status.caution)
        zone(90f, 60f, status.good)
        zone(-60f, -90f, status.good)
        zone(30f, -30f, status.good)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink.copy(alpha = 0.8f).toArgb()
            textAlign = Paint.Align.CENTER
            this.textSize = textSize
        }
        for (i in -90..90 step tickInterval) {
            val y = getY(i.toFloat())
            drawLine(
                ink.copy(alpha = 0.6f),
                Offset(0f, y),
                Offset(tickLength, y),
                tickStroke,
                StrokeCap.Round
            )
            if (i % labelInterval == 0) {
                val degreeText = formatter.formatDegrees(i.absoluteValue.toFloat())
                drawContext.canvas.nativeCanvas.drawText(
                    degreeText,
                    tickLength + textPaint.measureText(degreeText),
                    y,
                    textPaint
                )
            }
        }

        val start = startInclination?.let { getY(it) }
        val current = getY(inclination)
        if (start != null) {
            drawRect(
                color = needle.copy(alpha = 0.3f),
                topLeft = Offset(0f, min(start, current)),
                size = Size(size.width, (start - current).absoluteValue)
            )
        } else {
            drawLine(
                needle,
                Offset(0f, current),
                Offset(size.width, current),
                needleStroke,
                StrokeCap.Round
            )
        }
    }
}
