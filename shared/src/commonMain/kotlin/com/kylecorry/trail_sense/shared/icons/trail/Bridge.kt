package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_bridge.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Bridge: ImageVector
    get() {
        _Bridge?.let { return it }
        return ImageVector.Builder(
            name = "Bridge",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M17 5v1H5V5H0v12h5v-2c0-3.3 2.7-6 6-6s6 2.7 6 6v2h5V5h-5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Bridge = it }
    }

private var _Bridge: ImageVector? = null
