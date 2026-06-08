package com.kylecorry.trail_sense.tools.clinometer.ui

import android.content.Context
import android.graphics.Paint
import android.view.View
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.interpolation.Interpolation.map
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import kotlin.math.absoluteValue
import kotlin.math.min

class CameraClinometerView(context: Context) {

    var inclination by mutableStateOf(0f)
    var startInclination by mutableStateOf<Float?>(null)
    private var cameraVisible by mutableStateOf(true)

    val view = ComposeView(context).apply {
        setContent {
            TrailSenseTheme {
                Content()
            }
        }
    }

    var id: Int
        get() = view.id
        set(value) {
            view.id = value
        }

    var isVisible: Boolean
        get() = cameraVisible
        set(value) {
            cameraVisible = value
            view.visibility = if (value) View.VISIBLE else View.GONE
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
    val dialColor = Color(Resources.color(context, R.color.colorSecondary))
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

        fun zone(start: Float, end: Float, color: Int) {
            drawRect(
                color = Color(color).copy(alpha = 150f / 255f),
                topLeft = Offset(0f, min(getY(start), getY(end))),
                size = Size(tickLength, (getY(start) - getY(end)).absoluteValue)
            )
        }

        zone(45f, 30f, AppColor.Red.color)
        zone(-30f, -45f, AppColor.Red.color)
        zone(60f, 45f, AppColor.Yellow.color)
        zone(-45f, -60f, AppColor.Yellow.color)
        zone(90f, 60f, AppColor.Green.color)
        zone(-60f, -90f, AppColor.Green.color)
        zone(30f, -30f, AppColor.Green.color)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            this.textSize = textSize
        }
        for (i in -90..90 step tickInterval) {
            val y = getY(i.toFloat())
            drawLine(
                Color.White,
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
                color = Color.White.copy(alpha = 100f / 255f),
                topLeft = Offset(0f, min(start, current)),
                size = Size(size.width, (start - current).absoluteValue)
            )
        } else {
            drawLine(
                Color.White,
                Offset(0f, current),
                Offset(size.width, current),
                needleStroke,
                StrokeCap.Round
            )
        }
    }
}
