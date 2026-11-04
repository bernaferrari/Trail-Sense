package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_clothing.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryClothing: ImageVector
    get() {
        _CategoryClothing?.let { return it }
        return ImageVector.Builder(
            name = "CategoryClothing",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M16 21H8a1 1 0 0 1-1-1v-7.93l-1.3 1c-0.39 0.39-1.02 0.39-1.41 0l-2.83-2.78c-0.39-0.39-0.39-1.02 0-1.41L7.34 3H9c0 1.1 1.34 2 3 2s3-0.9 3-2h1.66l5.88 5.88c0.39 0.39 0.39 1.02 0 1.41l-2.83 2.83c-0.39 0.38-1.02 0.38-1.41 0l-1.3-1V20a1 1 0 0 1-1 1"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryClothing = it }
    }

private var _CategoryClothing: ImageVector? = null
