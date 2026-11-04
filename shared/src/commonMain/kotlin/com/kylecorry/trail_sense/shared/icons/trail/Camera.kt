package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_camera.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Camera: ImageVector
    get() {
        _Camera?.let { return it }
        return ImageVector.Builder(
            name = "Camera",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8.8 12a3.2 3.2 0 1 1 6.4 0 3.2 3.2 0 1 1-6.4 0"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        addPath(
            pathData = addPathNodes("M9 2L7.17 4H4C2.9 4 2 4.9 2 6v12c0 1.1 0.9 2 2 2h16c1.1 0 2-0.9 2-2V6c0-1.1-0.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Camera = it }
    }

private var _Camera: ImageVector? = null
