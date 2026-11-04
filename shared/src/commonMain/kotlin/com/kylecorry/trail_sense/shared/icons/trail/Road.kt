package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_road.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Road: ImageVector
    get() {
        _Road?.let { return it }
        return ImageVector.Builder(
            name = "Road",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M11 16h2v4h-2m0-10h2v4h-2m0-10h2v4h-2M4 22h16V2H4v20z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Road = it }
    }

private var _Road: ImageVector? = null
