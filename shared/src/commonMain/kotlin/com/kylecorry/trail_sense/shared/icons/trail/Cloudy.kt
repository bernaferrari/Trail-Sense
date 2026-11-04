package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/cloudy.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Cloudy: ImageVector
    get() {
        _Cloudy?.let { return it }
        return ImageVector.Builder(
            name = "Cloudy",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9.363 5.342C6.94 5.345 4.956 7.267 4.876 9.689 4.749 9.677 4.622 9.67 4.494 9.669 2.012 9.67 0 11.682 0 14.164s2.012 4.494 4.494 4.494h15.011c2.483 0 4.495-2.012 4.495-4.494 0-2.482-2.012-4.495-4.495-4.495l-0.041 0.002c-0.09-2.415-2.072-4.327-4.489-4.329-1.021 0-2.012 0.349-2.809 0.988-0.795-0.638-1.784-0.986-2.803-0.988z"),
            fill = SolidColor(Color(0xFFCCCCCC)),
        )
        addPath(
            pathData = addPathNodes("M11.704 8.026c-1.741 0.002-3.226 1.262-3.51 2.98-1.756 0.247-3.197 1.75-3.199 3.523 0 1.967 1.595 3.562 3.563 3.562h6.885c1.967 0 3.562-1.595 3.562-3.562 0-1.968-1.461-3.562-3.429-3.562-0.12 0-0.241 0.008-0.361 0.022-0.293-1.711-1.776-2.963-3.511-2.963z"),
            fill = SolidColor(Color(0xFFB3B3B3)),
        )
        }.build().also { _Cloudy = it }
    }

private var _Cloudy: ImageVector? = null
