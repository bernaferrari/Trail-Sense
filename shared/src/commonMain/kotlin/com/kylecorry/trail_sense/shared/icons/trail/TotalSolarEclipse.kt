package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_total_solar_eclipse.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TotalSolarEclipse: ImageVector
    get() {
        _TotalSolarEclipse?.let { return it }
        return ImageVector.Builder(
            name = "TotalSolarEclipse",
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
            pathData = addPathNodes("M14.2 89.2a75 75 0 1 1 150 0 75 75 0 1 1-150 0"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _TotalSolarEclipse = it }
    }

private var _TotalSolarEclipse: ImageVector? = null
