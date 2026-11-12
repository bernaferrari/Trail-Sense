package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.graphics.withSave
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.astronomy.eclipse.LunarEclipseShadow
import com.kylecorry.sol.science.astronomy.units.CelestialObservation
import com.kylecorry.trail_sense.shared.icons.toImageBitmap
import com.kylecorry.trail_sense.shared.icons.trail.Moon
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.tools.astronomy.domain.MoonTilt

class LunarEclipseImageMapper {

    fun getEclipseImage(
        moon: CelestialObservation,
        shadow: LunarEclipseShadow,
        width: Int,
        height: Int,
        tilt: MoonTilt? = null
    ): ImageBitmap {
        val output = ImageBitmap(width, height)
        val canvas = Canvas(output)
        canvas.withSave {
            tilt?.let {
                canvas.rotate(it.parallacticAngle, width / 2f, height / 2f)
            }
            canvas.drawImage(TrailIcons.Moon.toImageBitmap(width, height), Offset.Zero, Paint())
        }

        val shadowOffset = Trigonometry.getAngularOffset(
            moon.azimuth.value,
            moon.altitude,
            shadow.umbra.azimuth.value,
            shadow.umbra.altitude
        )
        val moonAngularDiameter = moon.angularDiameter ?: 0.5f
        val umbraAngularDiameter = shadow.umbra.angularDiameter ?: 0.5f
        val penumbraAngularDiameter = shadow.penumbra.angularDiameter ?: umbraAngularDiameter
        val scale = minOf(width, height) / moonAngularDiameter
        val centerX = width / 2f
        val centerY = height / 2f
        val shadowCenter = Offset(centerX + shadowOffset.x * scale, centerY - shadowOffset.y * scale)
        val umbraRadius = umbraAngularDiameter / 2f * scale
        val penumbraRadius = penumbraAngularDiameter / 2f * scale

        val moonRadius = minOf(width, height) / 2f + CLIP_BUFFER_PIXELS
        val moonClip = Path().apply {
            addOval(Rect(Offset(centerX, centerY), moonRadius))
        }
        canvas.withSave {
            canvas.clipPath(moonClip)
            canvas.drawCircle(shadowCenter, penumbraRadius, Paint().apply {
                isAntiAlias = true
                color = SHADOW_COLOR.copy(alpha = PENUMBRA_ALPHA / 255f)
            })
            canvas.drawCircle(shadowCenter, umbraRadius, Paint().apply {
                isAntiAlias = true
                color = SHADOW_COLOR
            })
        }

        return output
    }

    companion object {
        private val SHADOW_COLOR = Color(0x80FF6D00)
        private const val CLIP_BUFFER_PIXELS = 0.5f
        private const val PENUMBRA_ALPHA = 32
    }
}
