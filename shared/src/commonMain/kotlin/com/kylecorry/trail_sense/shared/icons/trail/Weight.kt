package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_weight.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Weight: ImageVector
    get() {
        _Weight?.let { return it }
        return ImageVector.Builder(
            name = "Weight",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 3a4 4 0 0 1 4 4c0 0.73-0.19 1.41-0.54 2H18c0.95 0 1.75 0.67 1.95 1.56C21.96 18.57 22 18.78 22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2c0-0.22 0.04-0.43 2.05-8.44C4.25 9.67 5.05 9 6 9h2.54C8.19 8.41 8 7.73 8 7a4 4 0 0 1 4-4m0 2a2 2 0 0 0-2 2 2 2 0 0 0 2 2 2 2 0 0 0 2-2 2 2 0 0 0-2-2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Weight = it }
    }

private var _Weight: ImageVector? = null
