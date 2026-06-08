package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun compassCalibrationView(
    context: Context,
    width: Int,
    height: Int
): View {
    return TrailSenseComposeRootView(
        context,
        ViewCompositionStrategy.DisposeOnDetachedFromWindow
    ) {
        CompassCalibrationContent()
    }.apply {
        layoutParams = ViewGroup.LayoutParams(width, height)
    }
}

@Composable
fun CompassCalibrationContent(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "compass_calibration")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "compass_calibration_progress"
    )
    val primary = androidx.compose.material3.MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Color(AppColor.Blue.color))

        val figure8Width = size.width * 0.8f / 2f
        val figure8Height = size.height * 0.8f / 2f
        val radius = min(size.width, size.height)
        val tMin = -(PI / 2).toFloat()
        val tMax = ((3 * PI) / 2).toFloat()
        val t = Interpolation.map(progress.value, 0f, 1f, tMin, tMax)

        val path = Path()
        var pathT = tMin
        var first = true
        while (pathT < tMax) {
            val point = figure8(pathT)
            val x = point.x * figure8Width + size.width / 2f
            val y = point.y * figure8Height + size.height / 2f
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
            pathT += 0.01f
        }
        drawPath(path, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(5.dp.toPx()))

        val pos = figure8(t).let {
            Vector2(it.x * figure8Width, it.y * figure8Height)
        }
        val nextPos = figure8(t + 0.001f).let {
            Vector2(it.x * figure8Width, it.y * figure8Height)
        }
        val heading = atan2(nextPos.y - pos.y, nextPos.x - pos.x).toDegrees()
        val phoneWidth = radius * 0.08f
        val phoneHeight = phoneWidth * 2f
        val screenWidth = phoneWidth * 0.8f
        val screenHeight = phoneHeight * 0.8f

        translate(size.width / 2f + pos.x, size.height / 2f + pos.y) {
            rotate(heading + 90f) {
                drawRect(
                    Color.Black,
                    topLeft = Offset(-phoneWidth / 2f, -phoneHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(phoneWidth, phoneHeight)
                )
                drawRect(
                    Color.White,
                    topLeft = Offset(-screenWidth / 2f, -screenHeight / 2f),
                    size = androidx.compose.ui.geometry.Size(screenWidth, screenHeight)
                )
                val arrowSize = phoneWidth * 0.75f
                val arrow = Path().apply {
                    moveTo(-arrowSize, 0f)
                    lineTo(0f, -arrowSize)
                    lineTo(arrowSize, 0f)
                    close()
                }
                drawPath(arrow, primary)
            }
        }
    }
}

private fun figure8(t: Float): Vector2 {
    val x = cos(t)
    val y = -sin(t) * cos(t)
    return Vector2(x, y)
}
