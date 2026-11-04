package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_food.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryFood: ImageVector
    get() {
        _CategoryFood?.let { return it }
        return ImageVector.Builder(
            name = "CategoryFood",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20 10c2 3-3 12-5 12s-2-1-3-1-1 1-3 1-7-9-5-12 5-3 7-2V5C5.38 8.07 4.11 3.78 4.11 3.78S6.77 0.19 11 5V3h2v5c2-1 5-1 7 2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryFood = it }
    }

private var _CategoryFood: ImageVector? = null
