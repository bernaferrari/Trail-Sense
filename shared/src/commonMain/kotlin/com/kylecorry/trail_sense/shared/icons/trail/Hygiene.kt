package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_hygiene.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Hygiene: ImageVector
    get() {
        _Hygiene?.let { return it }
        return ImageVector.Builder(
            name = "Hygiene",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12.91 6.43l2.12 2.12 1.06-1.05-1.06-1.07 2.83-2.83 1.06 1.06L20 3.6l-2.14-2.1M3 20.57L4.43 22 14.5 11.9l2.13-0.71 4.77-4.76c0.78-0.78 0.78-2.05 0-2.83l-5.85 5.84-2.12 0.71z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Hygiene = it }
    }

private var _Hygiene: ImageVector? = null
