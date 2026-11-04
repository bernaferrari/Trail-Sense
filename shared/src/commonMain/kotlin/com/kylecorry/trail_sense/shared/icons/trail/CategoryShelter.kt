package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_shelter.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryShelter: ImageVector
    get() {
        _CategoryShelter?.let { return it }
        return ImageVector.Builder(
            name = "CategoryShelter",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20 19L11 1 2 19H0v1h22v-1h-2zM6 19l5-10 5 10H6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryShelter = it }
    }

private var _CategoryShelter: ImageVector? = null
