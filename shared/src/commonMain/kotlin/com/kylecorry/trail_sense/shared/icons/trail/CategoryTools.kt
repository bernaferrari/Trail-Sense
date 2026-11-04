package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_tools.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryTools: ImageVector
    get() {
        _CategoryTools?.let { return it }
        return ImageVector.Builder(
            name = "CategoryTools",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M16.06 13.09l5.63 5.59-3.32 3.28-5.59-5.59v-0.92l2.36-2.36h0.92m0.91-2.53L16 9.6l-4.79 4.8v1.97L5.58 22 2.3 18.68l5.59-5.59h1.97l0.78-0.78L6.8 8.46H5.5L2.69 5.62 5.31 3l2.8 2.8v1.31L12 10.95l2.66-2.66-0.96-1.01L15 5.97h-2.66l-0.65-0.65L15 2l0.66 0.66v2.66L16.97 4l3.28 3.28c1.09 1.1 1.09 2.89 0 3.98l-1.97-2.01-1.31 1.31z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryTools = it }
    }

private var _CategoryTools: ImageVector? = null
