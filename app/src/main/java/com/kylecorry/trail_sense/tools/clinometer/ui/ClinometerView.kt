@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.clinometer.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.sol.math.trigonometry.Trigonometry.deltaAngle
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.compose.trailSenseStatusColors
import kotlin.math.abs
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

    /** The dial even while the phone is held the wrong way, so the screen's centre is never empty. */
    @Composable
    fun Preview(modifier: Modifier = Modifier) {
        ClinometerContent(
            angle = displayAngle,
            startAngle = null,
            modifier = modifier.fillMaxSize()
        )
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

/**
 * The side clinometer: a twelve-sided cookie dial in the primary tone. The avalanche bands run round the rim as
 * thick rounded arcs in the good, caution and error colours, the needle is a solid primary hand, and a held sweep
 * fills as a soft wedge.
 */
@Composable
private fun ClinometerContent(
    angle: Float,
    startAngle: Float?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = FormatService.getInstance(context)
    val scheme = MaterialTheme.colorScheme
    val status = trailSenseStatusColors()
    val ink = scheme.onPrimaryContainer
    val needleColor = scheme.primary
    val hub = scheme.onPrimary
    val good = status.good
    val caution = status.caution
    val danger = scheme.error
    val labelSize = with(LocalDensity.current) { 12.sp.toPx() }
    // Not animated: the angle wraps around the dial and a tween would spin the needle the long way.
    val animatedAngle = angle

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialShapes.Cookie12Sided.toShape(),
                color = scheme.primaryContainer
            ) {}
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(26.dp)
            ) {
                val radius = min(size.width, size.height) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val bandWidth = 10.dp.toPx()
                val bandRadius = radius - bandWidth / 2f
                val bandTopLeft = Offset(center.x - bandRadius, center.y - bandRadius)
                val bandSize = Size(bandRadius * 2, bandRadius * 2)

                fun band(start: Float, stop: Float, color: Color) {
                    // A hair of gap at each end lets the rounded caps read as separate bands.
                    val gap = 3f
                    drawArc(
                        color = color,
                        startAngle = min(start, stop) + gap,
                        sweepAngle = abs(stop - start) - gap * 2,
                        useCenter = false,
                        topLeft = bandTopLeft,
                        size = bandSize,
                        style = Stroke(width = bandWidth, cap = StrokeCap.Round)
                    )
                }

                // The avalanche zones: 30-45° high risk, 45-60° moderate, the rest low; mirrored on both sides.
                listOf(0f, 180f).forEach { base ->
                    listOf(1f, -1f).forEach { sign ->
                        band(base + sign * 30f, base + sign * 45f, danger)
                        band(base + sign * 45f, base + sign * 60f, caution)
                        band(base + sign * 60f, base + sign * 90f, good)
                    }
                    band(base - 30f, base + 30f, good)
                }

                // Ticks every 10°, longer every 30°.
                val tickOuter = radius - bandWidth - 8.dp.toPx()
                for (i in 0 until 360 step 10) {
                    val radians = Math.toRadians((i - 90).toDouble())
                    val major = i % 30 == 0
                    val length = if (major) 12.dp.toPx() else 6.dp.toPx()
                    val c = cos(radians).toFloat()
                    val s = sin(radians).toFloat()
                    drawLine(
                        ink.copy(alpha = if (major) 0.7f else 0.35f),
                        Offset(center.x + c * (tickOuter - length), center.y + s * (tickOuter - length)),
                        Offset(center.x + c * tickOuter, center.y + s * tickOuter),
                        if (major) 2.5.dp.toPx() else 1.5.dp.toPx(),
                        StrokeCap.Round
                    )
                }

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ink.copy(alpha = 0.8f).toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = labelSize
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    fontFeatureSettings = "tnum"
                }
                // Upright labels: 90° at the top and bottom, 0° at the sides.
                val labelRadius = tickOuter - 14.dp.toPx() - labelSize / 2f
                for (i in 0 until 360 step 30) {
                    val radians = Math.toRadians((i - 90).toDouble())
                    val fromHorizontal = abs(((i % 180) - 90).toFloat())
                    drawContext.canvas.nativeCanvas.drawText(
                        formatter.formatDegrees(fromHorizontal),
                        center.x + cos(radians).toFloat() * labelRadius,
                        center.y + sin(radians).toFloat() * labelRadius + labelSize / 3f,
                        paint
                    )
                }

                startAngle?.let {
                    drawArc(
                        color = needleColor.copy(alpha = 0.24f),
                        startAngle = it - 90f,
                        sweepAngle = deltaAngle(it, animatedAngle),
                        useCenter = true,
                        topLeft = Offset(center.x - tickOuter, center.y - tickOuter),
                        size = Size(tickOuter * 2, tickOuter * 2)
                    )
                }

                rotate(animatedAngle, center) {
                    drawLine(
                        needleColor,
                        Offset(center.x, center.y + radius * 0.14f),
                        Offset(center.x, center.y - tickOuter + 4.dp.toPx()),
                        7.dp.toPx(),
                        StrokeCap.Round
                    )
                }
                drawCircle(needleColor, 14.dp.toPx(), center)
                drawCircle(hub, 5.dp.toPx(), center)
            }
        }
    }
}
