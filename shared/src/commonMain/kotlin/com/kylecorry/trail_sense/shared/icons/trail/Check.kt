package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_check.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Check: ImageVector
    get() {
        _Check?.let { return it }
        return ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _Check = it }
    }

private var _Check: ImageVector? = null
