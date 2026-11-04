package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_lighthouse.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Lighthouse: ImageVector
    get() {
        _Lighthouse?.let { return it }
        return ImageVector.Builder(
            name = "Lighthouse",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8 8h6l2 14H6L8 8zM7 7h8v1H7V7zm4-7l4 3H7l4-3zm2 4l8-1v4l-8-1V4zM9 4L1 3v4l8-1V4z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Lighthouse = it }
    }

private var _Lighthouse: ImageVector? = null
