package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/thermometer_colored.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ThermometerColored: ImageVector
    get() {
        _ThermometerColored?.let { return it }
        return ImageVector.Builder(
            name = "ThermometerColored",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M15 13V5a3 3 0 0 0-6 0v8a5 5 0 1 0 6 0m-3-9a1 1 0 0 1 1 1v3h-2V5a1 1 0 0 1 1-1z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _ThermometerColored = it }
    }

private var _ThermometerColored: ImageVector? = null
