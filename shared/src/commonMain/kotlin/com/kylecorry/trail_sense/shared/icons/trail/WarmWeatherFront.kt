package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_warm_weather_front.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.WarmWeatherFront: ImageVector
    get() {
        _WarmWeatherFront?.let { return it }
        return ImageVector.Builder(
            name = "WarmWeatherFront",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0.506 13.398a0.499 0.499 0 1 0 0 0.998h22.988a0.499 0.499 0 1 0 0-0.998z"),
            fill = SolidColor(Color(0xFFEF5350)),
        )
        addPath(
            pathData = addPathNodes("M0.412 13.927a2.495 2.495 0 0 1 2.495-2.496 2.495 2.495 0 0 1 2.495 2.496H2.908zm17.996 0a2.495 2.495 0 0 1 2.496-2.496 2.495 2.495 0 0 1 2.495 2.496h-2.496zm-8.998 0a2.495 2.495 0 0 1 2.495-2.496 2.495 2.495 0 0 1 2.496 2.496h-2.496z"),
            fill = SolidColor(Color(0xFFEF5350)),
        )
        }.build().also { _WarmWeatherFront = it }
    }

private var _WarmWeatherFront: ImageVector? = null
