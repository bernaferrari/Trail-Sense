package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_trail_fork.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TrailFork: ImageVector
    get() {
        _TrailFork?.let { return it }
        return ImageVector.Builder(
            name = "TrailFork",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = true
        ).apply {
        addPath(
            pathData = addPathNodes("M14 4l2.29 2.29-2.88 2.88 1.42 1.42 2.88-2.88L20 10V4zm-4 0H4v6l2.29-2.29 4.71 4.7V20h2v-8.41l-5.29-5.3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _TrailFork = it }
    }

private var _TrailFork: ImageVector? = null
