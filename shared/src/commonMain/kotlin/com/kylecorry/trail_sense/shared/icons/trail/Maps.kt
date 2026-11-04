package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/maps.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Maps: ImageVector
    get() {
        _Maps?.let { return it }
        return ImageVector.Builder(
            name = "Maps",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20.5 3l-0.16 0.03L15 5.1 9 3 3.36 4.9C3.15 4.97 3 5.15 3 5.38V20.5C3 20.78 3.22 21 3.5 21l0.16-0.03L9 18.9l6 2.1 5.64-1.9c0.21-0.07 0.36-0.25 0.36-0.48V3.5C21 3.22 20.78 3 20.5 3zM15 19l-6-2.11V5l6 2.11V19z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Maps = it }
    }

private var _Maps: ImageVector? = null
