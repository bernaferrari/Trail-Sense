package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_water.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryWater: ImageVector
    get() {
        _CategoryWater?.let { return it }
        return ImageVector.Builder(
            name = "CategoryWater",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 20a6 6 0 0 1-6-6c0-4 6-10.75 6-10.75S18 10 18 14a6 6 0 0 1-6 6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryWater = it }
    }

private var _CategoryWater: ImageVector? = null
