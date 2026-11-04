package com.kylecorry.trail_sense.shared.icons.trail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** A sun with eight rounded rays: light, brightness, daylight. */
val TrailIcons.SunRays: ImageVector
    get() {
        _SunRays?.let { return it }
        return ImageVector.Builder(
            name = "SunRays",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 16.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 7.5f)
                close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    moveTo(12f + 7f * cos(a).toFloat(), 12f + 7f * sin(a).toFloat())
                    lineTo(12f + 9.5f * cos(a).toFloat(), 12f + 9.5f * sin(a).toFloat())
                }
            }
        }.build().also { _SunRays = it }
    }

private var _SunRays: ImageVector? = null
