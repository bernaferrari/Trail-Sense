package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/satellite.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Satellite: ImageVector
    get() {
        _Satellite?.let { return it }
        return ImageVector.Builder(
            name = "Satellite",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M11.62 1l5.66 5.67-2.12 2.12-2.12-2.12-1.42 1.42 2.33 2.32-1.16 1.17 0.45 0.46c0.93-0.43 2.07-0.27 2.83 0.5l-3.53 3.53c-0.77-0.76-0.93-1.9-0.5-2.83l-0.46-0.45-1.17 1.16-2.32-2.33-1.42 1.42 2.12 2.12-2.12 2.12L1 11.62 3.14 9.5l2.12 2.12 1.41-1.41-2.83-2.83c-0.78-0.78-0.78-2.05 0-2.83l0.71-0.71c0.78-0.78 2.05-0.78 2.83 0l2.83 2.83 1.41-1.41L9.5 3.14 11.62 1M18 14a4 4 0 0 1-4 4v-2a2 2 0 0 0 2-2h2m4 0a8 8 0 0 1-8 8v-2a6 6 0 0 0 6-6h2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Satellite = it }
    }

private var _Satellite: ImageVector? = null
