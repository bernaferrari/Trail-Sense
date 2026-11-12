package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.withSave
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.astronomy.units.CelestialObservation

class SolarEclipseImageMapper {

    fun getEclipseImage(
        sun: CelestialObservation,
        moon: CelestialObservation,
        isTotal: Boolean,
        width: Int,
        height: Int
    ): ImageBitmap {
        val output = ImageBitmap(width, height)
        val canvas = Canvas(output)
        val moonOffset = Trigonometry.getAngularOffset(
            sun.azimuth.value,
            sun.altitude,
            moon.azimuth.value,
            moon.altitude
        )
        val sunAngularDiameter = sun.angularDiameter ?: 0.5f
        val moonAngularDiameter = moon.angularDiameter ?: 0.5f

        val scale = minOf(width, height) / sunAngularDiameter
        val sunRadius = sunAngularDiameter / 2f * scale
        val moonRadius = moonAngularDiameter / 2f * scale
        val center = Offset(width / 2f, height / 2f)
        val moonCenter = Offset(center.x + moonOffset.x * scale, center.y - moonOffset.y * scale)

        canvas.drawCircle(center, sunRadius, Paint().apply {
            isAntiAlias = true
            color = SUN_COLOR
        })
        val moonPaint = Paint().apply {
            isAntiAlias = true
            color = MOON_COLOR
        }
        val sunClip = Path().apply {
            addOval(Rect(center, sunRadius + CLIP_BUFFER_PIXELS))
        }
        canvas.withSave {
            canvas.clipPath(sunClip)
            if (isTotal) {
                // Render the total eclipse as a ring of the sun with the moon centered
                canvas.drawCircle(center, sunRadius * TOTAL_ECLIPSE_MOON_PERCENT_DIAMETER, moonPaint)
            } else {
                canvas.drawCircle(moonCenter, moonRadius, moonPaint)
            }
        }

        return output
    }

    companion object {
        // The app's sun color (@color/sun)
        private val SUN_COLOR = Color(0xFFFFDD55)
        private val MOON_COLOR = Color.Black
        private const val TOTAL_ECLIPSE_MOON_PERCENT_DIAMETER = 0.95f
        private const val CLIP_BUFFER_PIXELS = 0.5f
    }
}
