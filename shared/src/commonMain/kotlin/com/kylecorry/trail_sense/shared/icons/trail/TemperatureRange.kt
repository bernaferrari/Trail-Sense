package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_temperature_range.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TemperatureRange: ImageVector
    get() {
        _TemperatureRange?.let { return it }
        return ImageVector.Builder(
            name = "TemperatureRange",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M11.049 13V5a3 3 0 0 0-6 0v8a5 5 0 1 0 6 0m-3-9a1 1 0 0 1 1 1v3h-2V5a1 1 0 0 1 1-1zm12.902 2.21L18.249 2l-2.702 4.21h1.428v11.579l-1.428 0.001L18.249 22l2.702-4.21h-1.428V6.21z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _TemperatureRange = it }
    }

private var _TemperatureRange: ImageVector? = null
