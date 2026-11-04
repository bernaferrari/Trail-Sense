package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_waterfall.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Waterfall: ImageVector
    get() {
        _Waterfall?.let { return it }
        return ImageVector.Builder(
            name = "Waterfall",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 30.0f,
            viewportHeight = 30.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M18 22c-2 0-4 2-6 2s-4-2-6-2-6 2-6 2v3s4-2 6-2 4 2 6 2 4-2 6-2 4 2 6 2 6-2 6-2v-3s-4 2-6 2-4-2-6-2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M13.7 22.5c-1-2.2-1.7-6.6-1.7-9.5 0-3-0.8-7.4-3.2-9.9C13.9 3.9 14 12.6 14 13c0 0.1 0.1 5 1.3 8.7 0.6-0.3 1.2-0.5 1.9-0.6-0.7-2.4-1.2-5.7-1.2-8.1 0-3-0.8-7.4-3.2-9.9C17.9 3.9 18 12.6 18 13c0 0.1 0.1 4.5 1.2 8.2 0.8 0.2 1.6 0.6 2.3 0.9 0 0 0.1 0 0.1 0.1-0.9-2.4-1.6-6.4-1.6-9.2 0-3-0.8-7.4-3.2-9.9C21.9 3.9 22 12.6 22 13c0 0.1 0.2 6.2 1.8 10H24c0.5 0 1.1-0.1 1.8-0.4-1-2.2-1.8-6.6-1.8-9.6s-0.8-7.4-3.2-9.9C25.9 3.9 26 12.6 26 13c0 0.1 0.2 5.3 1.4 9 0.7-0.3 1.4-0.6 1.8-0.8-0.7-2.3-1.2-5.7-1.2-8.2 0-4.5-1.7-12-8-12H5v0.1C4.7 1 4.3 1 4 1H1v2h3c5.9 0 6 9.6 6 10 0 0.1 0.2 6.2 1.8 10H12c0.5 0 1.1-0.2 1.7-0.5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Waterfall = it }
    }

private var _Waterfall: ImageVector? = null
