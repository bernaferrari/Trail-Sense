package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.withSave
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.trail_sense.shared.icons.toImageBitmap
import com.kylecorry.trail_sense.shared.icons.trail.Moon
import com.kylecorry.trail_sense.shared.icons.trail.MoonFirstQuarter
import com.kylecorry.trail_sense.shared.icons.trail.MoonNew
import com.kylecorry.trail_sense.shared.icons.trail.MoonThirdQuarter
import com.kylecorry.trail_sense.shared.icons.trail.MoonWaningCrescent
import com.kylecorry.trail_sense.shared.icons.trail.MoonWaningGibbous
import com.kylecorry.trail_sense.shared.icons.trail.MoonWaxingCrescent
import com.kylecorry.trail_sense.shared.icons.trail.MoonWaxingGibbous
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.tools.astronomy.domain.MoonTilt
import kotlin.math.abs

class MoonPhaseImageMapper {

    private val shadowPaint = Paint().apply {
        isAntiAlias = true
        color = Color(SHADOW_COLOR)
        blendMode = BlendMode.SrcAtop
    }

    private val litPaint = Paint().apply {
        isAntiAlias = true
        color = Color.White
    }

    private val plainShadowPaint = Paint().apply {
        isAntiAlias = true
        color = Color.Black
        blendMode = BlendMode.SrcAtop
    }

    fun getPhaseImage(phase: MoonTruePhase): ImageVector {
        return when (phase) {
            MoonTruePhase.FirstQuarter -> TrailIcons.MoonFirstQuarter
            MoonTruePhase.Full -> TrailIcons.Moon
            MoonTruePhase.ThirdQuarter -> TrailIcons.MoonThirdQuarter
            MoonTruePhase.New -> TrailIcons.MoonNew
            MoonTruePhase.WaningCrescent -> TrailIcons.MoonWaningCrescent
            MoonTruePhase.WaningGibbous -> TrailIcons.MoonWaningGibbous
            MoonTruePhase.WaxingCrescent -> TrailIcons.MoonWaxingCrescent
            MoonTruePhase.WaxingGibbous -> TrailIcons.MoonWaxingGibbous
        }
    }

    /**
     * The phase as an image. [plain] draws a clean disc (pure white lit face, opaque black shadow, a smooth
     * terminator) for screens that recolour it; otherwise it is the textured moon icon under a translucent shadow,
     * for widgets and the calendar.
     */
    fun getPhaseImage(
        phaseAngle: Float,
        width: Int,
        height: Int,
        tilt: MoonTilt? = null,
        plain: Boolean = false
    ): ImageBitmap {
        val output = ImageBitmap(width, height)
        val canvas = Canvas(output)
        canvas.withSave {
            tilt?.let {
                canvas.rotate(it.parallacticAngle, width / 2f, height / 2f)
            }
            if (plain) {
                canvas.drawCircle(Offset(width / 2f, height / 2f), minOf(width, height) / 2f, litPaint)
            } else {
                canvas.drawImage(TrailIcons.Moon.toImageBitmap(width, height), Offset.Zero, Paint())
            }

            tilt?.let {
                val rotationAdjustment = if (phaseAngle < 180) {
                    90f
                } else {
                    -90f
                }
                canvas.rotate(-(it.brightLimbAngleFromNorth + rotationAdjustment), width / 2f, height / 2f)
            }
            drawShadow(
                canvas,
                phaseAngle,
                width.toFloat(),
                height.toFloat(),
                if (plain) plainShadowPaint else shadowPaint
            )
        }
        return output
    }

    private fun drawShadow(
        canvas: Canvas,
        angle: Float,
        width: Float,
        height: Float,
        paint: Paint
    ) {
        when {
            Arithmetic.isZero(angle) || Arithmetic.isApproximatelyEqual(angle, 360f) -> {
                canvas.drawRect(0f, 0f, width, height, paint)
            }

            else -> {
                canvas.drawPath(createShadowPath(angle, width, height), paint)
            }
        }
    }

    private fun createShadowPath(angle: Float, width: Float, height: Float): Path {
        val centerX = width / 2f
        val terminatorRadius = abs(cosDegrees(angle)) * centerX
        val terminator = Rect(
            centerX - terminatorRadius,
            0f,
            centerX + terminatorRadius,
            height
        )
        val shadowOnLeft = angle < 180f
        val terminatorOnRight = (cosDegrees(angle) >= 0) == shadowOnLeft
        val offset = 0.5f
        return Path().apply {
            moveTo(centerX, -offset)
            lineTo(if (shadowOnLeft) -offset else width + offset, -offset)
            lineTo(if (shadowOnLeft) -offset else width + offset, height + offset)
            lineTo(centerX, height + offset)
            if (Arithmetic.isZero(terminatorRadius)) {
                lineTo(centerX, -offset)
            } else {
                arcTo(
                    terminator,
                    90f,
                    if (terminatorOnRight) -180f else 180f,
                    false
                )
            }
            close()
        }
    }

    companion object {
        private const val SHADOW_COLOR = 0xE0000000
    }
}
