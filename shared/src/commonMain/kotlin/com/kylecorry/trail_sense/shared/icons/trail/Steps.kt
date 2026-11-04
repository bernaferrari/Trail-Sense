package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/steps.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Steps: ImageVector
    get() {
        _Steps?.let { return it }
        return ImageVector.Builder(
            name = "Steps",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10.74 11.72c0.47 1.23 0.42 2.51-0.99 3.02-2.9 1.07-3.55-1.74-3.59-1.88l4.58-1.14m-5.03-0.81l4.32-1.07c-0.19-1.05 0.1-2.1 0.1-3.34 0-1.68-1.33-4.97-3.45-4.44C4.26 2.66 3.91 5.35 4 6.65c0.12 1.3 1.64 4.08 1.71 4.26m12.14 8.94c-0.03 0.15-0.69 2.95-3.59 1.89-1.4-0.52-1.46-1.8-0.99-3.03l4.58 1.14m2.15-6.2c0.1-1.3-0.24-4-2.67-4.6-2.11-0.55-3.44 2.76-3.44 4.45 0 1.23 0.28 2.28 0.11 3.33l4.3 1.07c0.08-0.18 1.59-2.96 1.7-4.25z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Steps = it }
    }

private var _Steps: ImageVector? = null
