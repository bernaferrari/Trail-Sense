package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_alert_simple.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.AlertSimple: ImageVector
    get() {
        _AlertSimple?.let { return it }
        return ImageVector.Builder(
            name = "AlertSimple",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8.6 19.5c0-1.5 1-2.5 2.4-2.5 1.5 0 2.4 1 2.4 2.5 0 1.4-1 2.5-2.4 2.5-1.5 0-2.5-1.1-2.4-2.5zM9.5 15L8.9 0h4.2l-0.6 15h-3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _AlertSimple = it }
    }

private var _AlertSimple: ImageVector? = null
