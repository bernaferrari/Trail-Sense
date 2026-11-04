package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_prev.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Prev: ImageVector
    get() {
        _Prev?.let { return it }
        return ImageVector.Builder(
            name = "Prev",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _Prev = it }
    }

private var _Prev: ImageVector? = null
