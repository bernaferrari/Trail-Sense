package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_backtrack.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolBacktrack: ImageVector
    get() {
        _ToolBacktrack?.let { return it }
        return ImageVector.Builder(
            name = "ToolBacktrack",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M18 15a3 3 0 0 1 3 3 3 3 0 0 1-3 3c-1.31 0-2.42-0.83-2.83-2H14v-2h1.17c0.41-1.17 1.52-2 2.83-2m0 2a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1m0-9a1.43 1.43 0 0 0 1.43-1.43c0-0.79-0.64-1.43-1.43-1.43-0.79 0-1.43 0.64-1.43 1.43A1.43 1.43 0 0 0 18 8m0-5.43a4 4 0 0 1 4 4C22 9.56 18 14 18 14s-4-4.44-4-7.43a4 4 0 0 1 4-4M8.83 17H10v2H8.83C8.42 20.17 7.31 21 6 21a3 3 0 0 1-3-3c0-1.31 0.83-2.42 2-2.83V14h2v1.17c0.85 0.3 1.53 0.98 1.83 1.83M6 17a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1M6 3a3 3 0 0 1 3 3c0 1.31-0.83 2.42-2 2.83V10H5V8.83C3.83 8.42 3 7.31 3 6a3 3 0 0 1 3-3m0 2a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1m5 14v-2h2v2h-2m-4-6H5v-2h2v2z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolBacktrack = it }
    }

private var _ToolBacktrack: ImageVector? = null
