package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/flashlight.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Flashlight: ImageVector
    get() {
        _Flashlight?.let { return it }
        return ImageVector.Builder(
            name = "Flashlight",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9 10L6 5h12l-3 5H9m9-6H6V2h12v2M9 22V11h6v11H9m3-9a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Flashlight = it }
    }

private var _Flashlight: ImageVector? = null
