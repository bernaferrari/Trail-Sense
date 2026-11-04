package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/sunny.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Sunny: ImageVector
    get() {
        _Sunny?.let { return it }
        return ImageVector.Builder(
            name = "Sunny",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M4.96 12a7.04 7.04 0 1 1 14.08 0 7.04 7.04 0 1 1-14.08 0"),
            fill = SolidColor(Color(0xFFFFDD55)),
        )
        }.build().also { _Sunny = it }
    }

private var _Sunny: ImageVector? = null
