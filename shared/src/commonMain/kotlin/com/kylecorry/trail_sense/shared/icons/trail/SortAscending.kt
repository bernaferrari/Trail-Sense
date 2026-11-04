package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/sort_ascending.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.SortAscending: ImageVector
    get() {
        _SortAscending?.let { return it }
        return ImageVector.Builder(
            name = "SortAscending",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19 17H22L18 21L14 17H17V3H19M2 17H12V19H2M6 5V7H2V5M2 11H9V13H2V11Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _SortAscending = it }
    }

private var _SortAscending: ImageVector? = null
