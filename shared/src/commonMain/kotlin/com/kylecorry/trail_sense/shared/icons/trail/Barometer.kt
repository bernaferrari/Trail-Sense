package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_barometer.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Barometer: ImageVector
    get() {
        _Barometer?.let { return it }
        return ImageVector.Builder(
            name = "Barometer",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 2A10 10 0 0 0 2 12a10 10 0 0 0 10 10 10 10 0 0 0 10-10A10 10 0 0 0 12 2m0 2a8 8 0 0 1 8 8c0 2.4-1 4.5-2.7 6-1.4-1.3-3.3-2-5.3-2s-3.8 0.7-5.3 2C5 16.5 4 14.4 4 12a8 8 0 0 1 8-8m2 1.89c-0.38 0.01-0.74 0.26-0.9 0.65l-1.29 3.23-0.1 0.23c-0.71 0.13-1.3 0.6-1.57 1.26-0.41 1.03 0.09 2.19 1.12 2.6 1.03 0.41 2.19-0.09 2.6-1.12 0.26-0.66 0.14-1.42-0.29-1.98l0.1-0.26 1.29-3.21 0.01-0.03c0.2-0.51-0.05-1.09-0.56-1.3-0.13-0.05-0.26-0.07-0.41-0.07M10 6a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1M7 9a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1m10 0a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Barometer = it }
    }

private var _Barometer: ImageVector? = null
