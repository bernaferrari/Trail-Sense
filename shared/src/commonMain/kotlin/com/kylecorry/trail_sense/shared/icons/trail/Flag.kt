package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/flag.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Flag: ImageVector
    get() {
        _Flag?.let { return it }
        return ImageVector.Builder(
            name = "Flag",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.4,6L14,4H5V21H7V14H12.6L13,16H20V6H14.4Z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _Flag = it }
    }

private var _Flag: ImageVector? = null
