package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/clinometer.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Clinometer: ImageVector
    get() {
        _Clinometer?.let { return it }
        return ImageVector.Builder(
            name = "Clinometer",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20 19H4.09L14.18 4.43l1.64 1.14-4.54 6.56c1.61 0.83 2.72 2.49 2.72 4.41 0 0.16 0 0.31-0.03 0.46H20v2M7.91 17h4.05C12 16.85 12 16.7 12 16.54c0-1.26-0.76-2.32-1.86-2.76L7.91 17z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Clinometer = it }
    }

private var _Clinometer: ImageVector? = null
