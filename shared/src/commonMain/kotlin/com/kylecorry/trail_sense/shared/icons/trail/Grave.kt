package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_grave.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Grave: ImageVector
    get() {
        _Grave?.let { return it }
        return ImageVector.Builder(
            name = "Grave",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10 2h4c3.31 0 5 2.69 5 6v10.66C16.88 17.63 15.07 17 12 17s-4.88 0.63-7 1.66V8c0-3.31 1.69-6 5-6M8 8v1.5h8V8H8m1 4v1.5h6V12H9M3 22v-0.69c2.66-1.69 10.23-5.47 18-0.06V22H3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Grave = it }
    }

private var _Grave: ImageVector? = null
