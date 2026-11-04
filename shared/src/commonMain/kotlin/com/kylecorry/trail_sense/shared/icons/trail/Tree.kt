package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/tree.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Tree: ImageVector
    get() {
        _Tree?.let { return it }
        return ImageVector.Builder(
            name = "Tree",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10 21v-3H3l5-5H5l5-5H7l5-5 5 5h-3l5 5h-3l5 5h-7v3h-4z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Tree = it }
    }

private var _Tree: ImageVector? = null
