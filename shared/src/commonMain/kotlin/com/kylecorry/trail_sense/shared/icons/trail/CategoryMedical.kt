package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_medical.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryMedical: ImageVector
    get() {
        _CategoryMedical?.let { return it }
        return ImageVector.Builder(
            name = "CategoryMedical",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M22 7h-7V0H7v7H0v8h7v7h8v-7h7z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryMedical = it }
    }

private var _CategoryMedical: ImageVector? = null
