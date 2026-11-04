package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_fullscreen_exit.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.FullscreenExit: ImageVector
    get() {
        _FullscreenExit?.let { return it }
        return ImageVector.Builder(
            name = "FullscreenExit",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _FullscreenExit = it }
    }

private var _FullscreenExit: ImageVector? = null
