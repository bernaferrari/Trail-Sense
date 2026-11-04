package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ruler.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Ruler: ImageVector
    get() {
        _Ruler?.let { return it }
        return ImageVector.Builder(
            name = "Ruler",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M1.39 18.36l1.77-1.76L4.58 18l1.06-1.05-1.42-1.41 1.42-1.42 2.47 2.48 1.06-1.06-2.47-2.48 1.41-1.41 1.42 1.41L10.59 12l-1.42-1.41 1.42-1.42 2.47 2.48 1.06-1.06-2.47-2.48 1.41-1.41 1.41 1.41 1.07-1.06-1.42-1.41 1.42-1.42L18 6.7l1.07-1.06-2.47-2.48 1.76-1.77 4.25 4.25L5.64 22.61l-4.25-4.25z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Ruler = it }
    }

private var _Ruler: ImageVector? = null
