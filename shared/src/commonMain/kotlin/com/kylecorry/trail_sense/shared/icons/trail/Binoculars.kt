package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_binoculars.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Binoculars: ImageVector
    get() {
        _Binoculars?.let { return it }
        return ImageVector.Builder(
            name = "Binoculars",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.96 14.4c-0.4 0-0.56-0.32-0.56-0.8V4.88h6.4l3.2 12v6.24c0 0.4-0.32 0.8-0.8 0.8h-6.72c-0.4 0-0.48-0.32-0.48-0.8V14.4zm-5.92 0c0.4 0 0.56-0.32 0.56-0.8V4.88H3.2l-3.2 12v6.24c0 0.4 0.32 0.8 0.8 0.8h6.72c0.4 0 0.56-0.32 0.56-0.8V14.4zM5.6 0.08h4v3.2h-4zm8.8 0h4v3.2h-4zm-3.2 4.8h1.6v8.8h-1.6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Binoculars = it }
    }

private var _Binoculars: ImageVector? = null
