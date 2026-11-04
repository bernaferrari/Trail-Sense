package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_theme.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Theme: ImageVector
    get() {
        _Theme?.let { return it }
        return ImageVector.Builder(
            name = "Theme",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M18 4V3c0-0.55-0.45-1-1-1H5C4.45 2 4 2.45 4 3v4c0 0.55 0.45 1 1 1h12c0.55 0 1-0.45 1-1V6h1v4H9v11c0 0.55 0.45 1 1 1h2c0.55 0 1-0.45 1-1v-9h8V4h-3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Theme = it }
    }

private var _Theme: ImageVector? = null
