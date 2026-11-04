package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_baseline_stop_24.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.BaselineStop24: ImageVector
    get() {
        _BaselineStop24?.let { return it }
        return ImageVector.Builder(
            name = "BaselineStop24",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M6 6h12v12H6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _BaselineStop24 = it }
    }

private var _BaselineStop24: ImageVector? = null
