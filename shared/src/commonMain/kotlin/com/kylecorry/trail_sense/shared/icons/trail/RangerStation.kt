package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ranger_station.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.RangerStation: ImageVector
    get() {
        _RangerStation?.let { return it }
        return ImageVector.Builder(
            name = "RangerStation",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20,13l-7,-4.7l0,-4.3l2,0l0,1l3,0l0,-4l-3,0l0,-1l-2,0l-1,0l0,7.7l-1,-0.7l-9,6l-1,0l0,1l2,0l0,8l6,0l0,-6l4,0l0,6l6,0l0,-8l2,0l0,-1z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _RangerStation = it }
    }

private var _RangerStation: ImageVector? = null
