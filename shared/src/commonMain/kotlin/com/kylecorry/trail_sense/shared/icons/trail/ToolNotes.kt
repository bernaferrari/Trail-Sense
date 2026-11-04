package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_notes.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolNotes: ImageVector
    get() {
        _ToolNotes?.let { return it }
        return ImageVector.Builder(
            name = "ToolNotes",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19 3H4.99C3.89 3 3 3.9 3 5l0.01 14c0 1.1 0.89 2 1.99 2h10l6-6V5c0-1.1-0.9-2-2-2zM7 8h10v2H7V8zm5 6H7v-2h5v2zm2 5.5V14h5.5L14 19.5z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolNotes = it }
    }

private var _ToolNotes: ImageVector? = null
