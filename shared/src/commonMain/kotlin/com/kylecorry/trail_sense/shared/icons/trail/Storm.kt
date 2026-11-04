package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/storm.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Storm: ImageVector
    get() {
        _Storm?.let { return it }
        return ImageVector.Builder(
            name = "Storm",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9.363 0.313C6.94 0.316 4.956 2.238 4.876 4.66 4.749 4.648 4.622 4.642 4.495 4.641c-2.482 0-4.494 2.012-4.494 4.494 0 2.482 2.012 4.494 4.494 4.494h15.011c2.482 0 4.495-2.012 4.495-4.494 0-2.482-2.012-4.494-4.495-4.494l-0.042 0.002c-0.089-2.415-2.072-4.327-4.489-4.329-1.021 0-2.012 0.349-2.809 0.987-0.795-0.637-1.784-0.985-2.803-0.987z"),
            fill = SolidColor(Color(0xFFCCCCCC)),
        )
        addPath(
            pathData = addPathNodes("M11.704 2.997c-1.741 0.002-3.225 1.262-3.51 2.979-1.756 0.248-3.196 1.75-3.198 3.523 0 1.967 1.595 3.562 3.562 3.562h6.885c1.967 0 3.562-1.595 3.562-3.562 0-1.967-1.461-3.562-3.428-3.562-0.121 0.001-0.242 0.009-0.362 0.022-0.292-1.711-1.775-2.963-3.511-2.963z"),
            fill = SolidColor(Color(0xFFB3B3B3)),
        )
        addPath(
            pathData = addPathNodes("M10 14.687v4.95h1.35v4.05l3.15-5.4h-1.8l1.8-3.6z"),
            fill = SolidColor(Color(0xFFFFDD55)),
        )
        }.build().also { _Storm = it }
    }

private var _Storm: ImageVector? = null
