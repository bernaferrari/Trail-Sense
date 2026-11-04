package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_natural.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryNatural: ImageVector
    get() {
        _CategoryNatural?.let { return it }
        return ImageVector.Builder(
            name = "CategoryNatural",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M17 8C8 10 5.9 16.17 3.82 21.34L5.71 22l0.95-2.3C7.14 19.87 7.64 20 8 20 19 20 22 3 22 3c-1 2-8 2.25-13 3.25S2 11.5 2 13.5s1.75 3.75 1.75 3.75C7 8 17 8 17 8z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryNatural = it }
    }

private var _CategoryNatural: ImageVector? = null
