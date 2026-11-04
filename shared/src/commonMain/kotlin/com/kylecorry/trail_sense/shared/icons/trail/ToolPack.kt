package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_pack.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolPack: ImageVector
    get() {
        _ToolPack?.let { return it }
        return ImageVector.Builder(
            name = "ToolPack",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M16 5V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v1a4 4 0 0 0-4 4v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9a4 4 0 0 0-4-4m-6-1h4v1h-4V4m2 5l2 2-2 2-2-2 2-2m6 7H9v2H8v-2H6v-1h12v1z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolPack = it }
    }

private var _ToolPack: ImageVector? = null
