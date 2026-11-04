package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_delete.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Delete: ImageVector
    get() {
        _Delete?.let { return it }
        return ImageVector.Builder(
            name = "Delete",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M6 19c0 1.1 0.9 2 2 2h8c1.1 0 2-0.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Delete = it }
    }

private var _Delete: ImageVector? = null
