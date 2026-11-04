package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_steady_arrow.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.SteadyArrow: ImageVector
    get() {
        _SteadyArrow?.let { return it }
        return ImageVector.Builder(
            name = "SteadyArrow",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M5.052 8.757L0 12l5.052 3.243v-1.714h13.896v1.714L24 12l-5.052-3.243v1.714H5.052z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _SteadyArrow = it }
    }

private var _SteadyArrow: ImageVector? = null
