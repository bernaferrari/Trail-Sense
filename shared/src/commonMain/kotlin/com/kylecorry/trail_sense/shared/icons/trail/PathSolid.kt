package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/path_solid.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PathSolid: ImageVector
    get() {
        _PathSolid?.let { return it }
        return ImageVector.Builder(
            name = "PathSolid",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M4 9h16a4 3 0 0 1 4 3 4 3 0 0 1-4 3H4a4 3 0 0 1-4-3 4 3 0 0 1 4-3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _PathSolid = it }
    }

private var _PathSolid: ImageVector? = null
