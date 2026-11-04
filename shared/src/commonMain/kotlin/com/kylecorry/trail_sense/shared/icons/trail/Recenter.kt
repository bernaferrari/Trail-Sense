package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_recenter.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Recenter: ImageVector
    get() {
        _Recenter?.let { return it }
        return ImageVector.Builder(
            name = "Recenter",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M3 5v4h2V5h4V3H5C3.9 3 3 3.9 3 5zm2 10H3v4c0 1.1 0.9 2 2 2h4v-2H5v-4zm14 4h-4v2h4c1.1 0 2-0.9 2-2v-4h-2v4zm0-16h-4v2h4v4h2V5c0-1.1-0.9-2-2-2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Recenter = it }
    }

private var _Recenter: ImageVector? = null
