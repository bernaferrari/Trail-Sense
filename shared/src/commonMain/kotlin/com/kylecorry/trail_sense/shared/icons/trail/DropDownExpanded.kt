package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_drop_down_expanded.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.DropDownExpanded: ImageVector
    get() {
        _DropDownExpanded?.let { return it }
        return ImageVector.Builder(
            name = "DropDownExpanded",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        group(rotate = 180.0f, pivotX = 12.0f, pivotY = 12.0f) {
            addPath(
                pathData = addPathNodes("M7 10l5 5 5-5z"),
                fill = SolidColor(Color(0xFFFFFFFF)),
            )
        }
        }.build().also { _DropDownExpanded = it }
    }

private var _DropDownExpanded: ImageVector? = null
