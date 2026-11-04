package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_add.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Add: ImageVector
    get() {
        _Add?.let { return it }
        return ImageVector.Builder(
            name = "Add",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Add = it }
    }

private var _Add: ImageVector? = null
