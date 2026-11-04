package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_zoom_out.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ZoomOut: ImageVector
    get() {
        _ZoomOut?.let { return it }
        return ImageVector.Builder(
            name = "ZoomOut",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M4 11h16v2H4z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
            pathFillType = PathFillType.EvenOdd,
        )
        }.build().also { _ZoomOut = it }
    }

private var _ZoomOut: ImageVector? = null
