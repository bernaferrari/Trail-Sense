package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_whistle.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolWhistle: ImageVector
    get() {
        _ToolWhistle?.let { return it }
        return ImageVector.Builder(
            name = "ToolWhistle",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8.5 9A6.5 6.5 0 0 0 2 15.5 6.5 6.5 0 0 0 8.5 22a6.5 6.5 0 0 0 6.5-6.5v-1.59L22 12V9H11v2H9V9H8.5M11 2v5H9V2h2M6.35 7.28C5.68 7.44 5.04 7.68 4.43 8L2.14 4.88 3.76 3.7l2.59 3.58m11.51-2.4L16.32 7h-2.47l2.39-3.3 1.62 1.18z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolWhistle = it }
    }

private var _ToolWhistle: ImageVector? = null
