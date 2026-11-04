package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tide_table.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TideTable: ImageVector
    get() {
        _TideTable?.let { return it }
        return ImageVector.Builder(
            name = "TideTable",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20 12h2v2h-2c-1.38 0-2.74-0.35-4-1-2.5 1.3-5.5 1.3-8 0-1.26 0.65-2.63 1-4 1H2v-2h2c1.39 0 2.78-0.47 4-1.33 2.44 1.71 5.56 1.71 8 0 1.22 0.86 2.61 1.33 4 1.33m0-6h2v2h-2c-1.38 0-2.74-0.35-4-1-2.5 1.3-5.5 1.3-8 0-1.26 0.65-2.63 1-4 1H2V6h2c1.39 0 2.78-0.47 4-1.33 2.44 1.71 5.56 1.71 8 0C17.22 5.53 18.61 6 20 6m0 12h2v2h-2c-1.38 0-2.74-0.35-4-1-2.5 1.3-5.5 1.3-8 0-1.26 0.65-2.63 1-4 1H2v-2h2c1.39 0 2.78-0.47 4-1.33 2.44 1.71 5.56 1.71 8 0 1.22 0.86 2.61 1.33 4 1.33z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _TideTable = it }
    }

private var _TideTable: ImageVector? = null
