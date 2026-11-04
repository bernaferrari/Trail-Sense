package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_partial_solar_eclipse.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PartialSolarEclipse: ImageVector
    get() {
        _PartialSolarEclipse?.let { return it }
        return ImageVector.Builder(
            name = "PartialSolarEclipse",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 178.4f,
            viewportHeight = 178.4f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 89.2a89.2 89.2 0 1 1 178.4 0A89.2 89.2 0 1 1 0 89.2"),
            fill = SolidColor(Color(0xFFFFDD55)),
        )
        addPath(
            pathData = addPathNodes("M124.44 29.77a89.2 89.2 0 0 0-89.21 89.2 89.2 89.2 0 0 0 16.05 50.85 89.2 89.2 0 0 0 37.92 8.58 89.2 89.2 0 0 0 89.2-89.2 89.2 89.2 45 0 0-16.04-50.85 89.2 89.2 45 0 0-37.92-8.58z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _PartialSolarEclipse = it }
    }

private var _PartialSolarEclipse: ImageVector? = null
