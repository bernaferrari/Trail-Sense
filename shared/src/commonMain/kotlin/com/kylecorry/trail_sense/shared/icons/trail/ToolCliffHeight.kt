package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_cliff_height.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolCliffHeight: ImageVector
    get() {
        _ToolCliffHeight?.let { return it }
        return ImageVector.Builder(
            name = "ToolCliffHeight",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19.92 12.08L12 20l-7.92-7.92 1.42-1.41 5.5 5.5V2h2v14.17l5.5-5.51 1.42 1.42M12 20H2v2h20v-2H12z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolCliffHeight = it }
    }

private var _ToolCliffHeight: ImageVector? = null
