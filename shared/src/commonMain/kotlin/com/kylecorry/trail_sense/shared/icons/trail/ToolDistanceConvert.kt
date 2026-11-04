package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_distance_convert.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolDistanceConvert: ImageVector
    get() {
        _ToolDistanceConvert?.let { return it }
        return ImageVector.Builder(
            name = "ToolDistanceConvert",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M21 9l-4-4v3h-7v2h7v3M7 11l-4 4 4 4v-3h7v-2H7v-3z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolDistanceConvert = it }
    }

private var _ToolDistanceConvert: ImageVector? = null
