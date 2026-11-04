package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_cabin.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Cabin: ImageVector
    get() {
        _Cabin?.let { return it }
        return ImageVector.Builder(
            name = "Cabin",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 30.0f,
            viewportHeight = 30.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 12h30v4H0zm0 14h12v4H0zm18 0h12v4H18zM0 19h12v4H0zm18 0h12v4H18zM0 9h30L15 1z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Cabin = it }
    }

private var _Cabin: ImageVector? = null
