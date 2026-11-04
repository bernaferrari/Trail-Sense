package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_cold_weather_front.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ColdWeatherFront: ImageVector
    get() {
        _ColdWeatherFront?.let { return it }
        return ImageVector.Builder(
            name = "ColdWeatherFront",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0.506 13.398a0.499 0.499 45 1 0 0 0.998h22.988a0.499 0.499 45 1 0 0-0.998z"),
            fill = SolidColor(Color(0xFFA0CAFF)),
        )
        addPath(
            pathData = addPathNodes("M3.002 9.604l2.496 4.323H0.507zm17.996 0l2.496 4.323h-4.991zm-8.998 0l2.496 4.323H9.505z"),
            fill = SolidColor(Color(0xFFA0CAFF)),
        )
        }.build().also { _ColdWeatherFront = it }
    }

private var _ColdWeatherFront: ImageVector? = null
