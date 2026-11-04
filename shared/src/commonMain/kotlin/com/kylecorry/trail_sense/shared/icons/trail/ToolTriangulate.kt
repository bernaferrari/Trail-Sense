package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_triangulate.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolTriangulate: ImageVector
    get() {
        _ToolTriangulate?.let { return it }
        return ImageVector.Builder(
            name = "ToolTriangulate",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9 3v6h0.73l-3.94 7H2v6h6v-2h8v2h6v-6h-3.79l-3.94-7H15V3m-4 2h2v2h-2m1 2.04l4 7.11V18H8v-1.85M4 18h2v2H4m14-2h2v2h-2"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolTriangulate = it }
    }

private var _ToolTriangulate: ImageVector? = null
