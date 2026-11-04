package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_pause.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Pause: ImageVector
    get() {
        _Pause?.let { return it }
        return ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14,19H18V5H14M6,19H10V5H6V19Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Pause = it }
    }

private var _Pause: ImageVector? = null
