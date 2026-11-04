package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/bench.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Bench: ImageVector
    get() {
        _Bench?.let { return it }
        return ImageVector.Builder(
            name = "Bench",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M23 13H1V15H3V19H5V15H19V19H21V15H23V13Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Bench = it }
    }

private var _Bench: ImageVector? = null
