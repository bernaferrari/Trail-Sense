package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/path_diamond.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PathDiamond: ImageVector
    get() {
        _PathDiamond?.let { return it }
        return ImageVector.Builder(
            name = "PathDiamond",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 12l3-3 3 3-3 3zm9 0l3-3 3 3-3 3zm9 0l3-3 3 3-3 3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _PathDiamond = it }
    }

private var _PathDiamond: ImageVector? = null
