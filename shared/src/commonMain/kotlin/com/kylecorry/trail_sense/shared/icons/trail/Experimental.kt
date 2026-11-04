package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_experimental.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Experimental: ImageVector
    get() {
        _Experimental?.let { return it }
        return ImageVector.Builder(
            name = "Experimental",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19.8 18.4L14 10.67V6.5l1.35-1.69C15.61 4.48 15.38 4 14.96 4H9.04C8.62 4 8.39 4.48 8.65 4.81L10 6.5v4.17L4.2 18.4C3.71 19.06 4.18 20 5 20h14c0.82 0 1.29-0.94 0.8-1.6z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Experimental = it }
    }

private var _Experimental: ImageVector? = null
