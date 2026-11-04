package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/cloud.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Cloud: ImageVector
    get() {
        _Cloud?.let { return it }
        return ImageVector.Builder(
            name = "Cloud",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9.363 5.342C6.94 5.345 4.956 7.267 4.876 9.689 4.749 9.677 4.622 9.67 4.494 9.669 2.012 9.67 0 11.682 0 14.164s2.012 4.494 4.494 4.494h15.011c2.483 0 4.495-2.012 4.495-4.494 0-2.482-2.012-4.495-4.495-4.495l-0.041 0.002c-0.09-2.415-2.072-4.327-4.489-4.329-1.021 0-2.012 0.349-2.809 0.988-0.795-0.638-1.784-0.986-2.803-0.988z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Cloud = it }
    }

private var _Cloud: ImageVector? = null
