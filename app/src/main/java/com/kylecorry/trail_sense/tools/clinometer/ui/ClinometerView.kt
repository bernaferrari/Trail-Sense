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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class ClinometerView : IClinometerView {

    private var displayAngle by mutableStateOf(90f)
    private var displayStartAngle by mutableStateOf<Float?>(null)
    private var viewInvisible by mutableStateOf(false)

    var isInvisible: Boolean
        get() = viewInvisible
        set(value) {
            viewInvisible = value
        }

    override var angle: Float
        get() = displayAngle - 90f
        set(value) {
            displayAngle = value + 90f
        }

    override var startAngle: Float?
        get() = displayStartAngle?.minus(90f)
        set(value) {
            displayStartAngle = value?.plus(90f)
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (viewInvisible) {
            return
        }
        ClinometerContent(
            angle = displayAngle,
            startAngle = displayStartAngle,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ClinometerContent(
    angle: Float,
    startAngle: Float?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = FormatService.getInstance(context)
    val dialColor = Color(Resources.color(context, R.color.colorSecondary))
    val density = LocalDensity.current
    val tickInterval = 10
    val labelInterval = 30
    val needlePercent = 0.75f
    val dividerPercent = 0.8f
    val avalancheIndicatorPercent = 0.97f
    val tickRadiusPercent = 0.9f
    val tickLengthPercent = 0.03f
    val tickStroke = with(density) { 2.dp.toPx() }
    val needleStroke = with(density) { 4.dp.toPx() }
    val centerStroke = with(density) { 1.dp.toPx() }
    val centerRadius = with(density) { 6.dp.toPx() }
    val textSize = with(density) { 10.sp.toPx() }

    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val tickLength = radius * tickLengthPercent
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        drawCircle(dialColor, radius, center)
        drawCircle(
            Color.White.copy(alpha = 10f / 255f),
            radius * dividerPercent,
            center,
            style = Stroke(width = 3f)
        )

        fun avalancheZone(start: Float, stop: Float, color: Int) {
            drawArc(
                color = Color(color).copy(alpha = 150f / 255f),
                startAngle = start,
                sweepAngle = stop - start,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = radius * (1 - avalancheIndicatorPercent) * 2f)
            )
        }

        avalancheZone(30f, 45f, AppColor.Red.color)
        avalancheZone(-30f, -45f, AppColor.Red.color)
        avalancheZone(210f, 225f, AppColor.Red.color)
        avalancheZone(-210f, -225f, AppColor.Red.color)
        avalancheZone(45f, 60f, AppColor.Yellow.color)
        avalancheZone(-45f, -60f, AppColor.Yellow.color)
        avalancheZone(225f, 240f, AppColor.Yellow.color)
        avalancheZone(-225f, -240f, AppColor.Yellow.color)
        avalancheZone(-30f, 30f, AppColor.Green.color)
        avalancheZone(-60f, -90f, AppColor.Green.color)
        avalancheZone(60f, 90f, AppColor.Green.color)
        avalancheZone(-210f, -150f, AppColor.Green.color)
        avalancheZone(-240f, -270f, AppColor.Green.color)
        avalancheZone(240f, 270f, AppColor.Green.color)

        for (i in 0 until 360 step tickInterval) {
            val radians = Math.toRadians((i - 90).toDouble())
            val tickRadius = radius * tickRadiusPercent
            val start = Offset(
                center.x + cos(radians).toFloat() * (tickRadius - tickLength / 2f),
                center.y + sin(radians).toFloat() * (tickRadius - tickLength / 2f)
            )
            val end = Offset(
                center.x + cos(radians).toFloat() * (tickRadius + tickLength / 2f),
                center.y + sin(radians).toFloat() * (tickRadius + tickLength / 2f)
            )
            drawLine(Color.White, start, end, tickStroke, StrokeCap.Round)
        }

        fun drawLabels(rotationOffset: Float = 0f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                this.textSize = textSize
            }
            for (i in 0..180 step labelInterval) {
                val degrees = if (i <= 90) 90 - i else i - 90
                val text = formatter.formatDegrees(degrees.toFloat())
                val x = center.x
                val y = center.y - radius * dividerPercent + tickLength + paint.textSize
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate(i + rotationOffset, center.x, center.y)
                    rotate(180f, x, y)
                    drawText(text, x, y, paint)
                    restore()
                }
            }
        }

        startAngle?.let {
            val delta = deltaAngle(it, angle)
            drawArc(
                color = Color.White.copy(alpha = 127f / 255f),
                startAngle = it - 90f,
                sweepAngle = delta,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
        }

        drawLabels()
        drawLabels(180f)

        val overhang = radius * 0.1f
        rotate(angle, center) {
            drawLine(
                Color.White,
                Offset(center.x, center.y + overhang),
                Offset(center.x, center.y - radius * needlePercent),
                needleStroke,
                StrokeCap.Round
            )
        }

        drawCircle(Color.White, centerRadius, center)
        drawCircle(dialColor, centerRadius, center, style = Stroke(width = centerStroke))
    }
}
