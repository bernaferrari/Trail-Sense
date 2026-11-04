package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_house.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.House: ImageVector
    get() {
        _House?.let { return it }
        return ImageVector.Builder(
            name = "House",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _House = it }
    }

private var _House: ImageVector? = null
