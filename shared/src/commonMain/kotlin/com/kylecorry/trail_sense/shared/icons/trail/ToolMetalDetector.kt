package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_metal_detector.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolMetalDetector: ImageVector
    get() {
        _ToolMetalDetector?.let { return it }
        return ImageVector.Builder(
            name = "ToolMetalDetector",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M3 7v6a9 9 0 0 0 9 9 9 9 0 0 0 9-9V7h-4v6a5 5 0 0 1-5 5 5 5 0 0 1-5-5V7m10-2h4V2h-4M3 5h4V2H3"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolMetalDetector = it }
    }

private var _ToolMetalDetector: ImageVector? = null
