package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/wind.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Wind: ImageVector
    get() {
        _Wind?.let { return it }
        return ImageVector.Builder(
            name = "Wind",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M4 10a1 1 0 0 1-1-1 1 1 0 0 1 1-1h8a2 2 0 0 0 2-2 2 2 0 0 0-2-2c-0.55 0-1.05 0.22-1.41 0.59C10.2 5 9.56 5 9.17 4.59c-0.39-0.39-0.39-1.03 0-1.42C9.9 2.45 10.9 2 12 2a4 4 0 0 1 4 4 4 4 0 0 1-4 4H4m15 2a1 1 0 0 0 1-1 1 1 0 0 0-1-1c-0.28 0-0.53 0.11-0.71 0.29-0.39 0.39-1.02 0.39-1.41 0-0.38-0.39-0.38-1.02 0-1.41C17.42 8.34 18.17 8 19 8a3 3 0 0 1 3 3 3 3 0 0 1-3 3H5a1 1 0 0 1-1-1 1 1 0 0 1 1-1h14m-1 6H4a1 1 0 0 1-1-1 1 1 0 0 1 1-1h14a3 3 0 0 1 3 3 3 3 0 0 1-3 3c-0.83 0-1.58-0.34-2.12-0.88-0.38-0.39-0.38-1.02 0-1.41 0.39-0.39 1.02-0.39 1.41 0C17.47 19.89 17.72 20 18 20a1 1 0 0 0 1-1 1 1 0 0 0-1-1z"),
            fill = SolidColor(Color(0xFFB3B3B3)),
        )
        }.build().also { _Wind = it }
    }

private var _Wind: ImageVector? = null
