package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/path_square.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PathSquare: ImageVector
    get() {
        _PathSquare?.let { return it }
        return ImageVector.Builder(
            name = "PathSquare",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9 9h6v6H9zM0 9h6v6H0zm18 0h6v6h-6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _PathSquare = it }
    }

private var _PathSquare: ImageVector? = null
