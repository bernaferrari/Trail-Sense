package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_minus.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Minus: ImageVector
    get() {
        _Minus?.let { return it }
        return ImageVector.Builder(
            name = "Minus",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19,13H5V11H19V13Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Minus = it }
    }

private var _Minus: ImageVector? = null
