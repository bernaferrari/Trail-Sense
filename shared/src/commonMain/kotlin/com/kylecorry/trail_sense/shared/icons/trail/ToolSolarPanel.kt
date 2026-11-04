package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_solar_panel.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolSolarPanel: ImageVector
    get() {
        _ToolSolarPanel?.let { return it }
        return ImageVector.Builder(
            name = "ToolSolarPanel",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M4 2h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-5v4h3v2h-5v-6h-2v6H6v-2h3v-4H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2m0 2v4h7V4H4m0 10h7v-4H4v4m16 0v-4h-7v4h7m0-10h-7v4h7V4z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolSolarPanel = it }
    }

private var _ToolSolarPanel: ImageVector? = null
