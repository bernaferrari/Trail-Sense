package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/path_dotted.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PathDotted: ImageVector
    get() {
        _PathDotted?.let { return it }
        return ImageVector.Builder(
            name = "PathDotted",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 12a3 3 0 1 1 6 0 3 3 0 1 1-6 0m9 0a3 3 0 1 1 6 0 3 3 0 1 1-6 0m9 0a3 3 0 1 1 6 0 3 3 0 1 1-6 0"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _PathDotted = it }
    }

private var _PathDotted: ImageVector? = null
