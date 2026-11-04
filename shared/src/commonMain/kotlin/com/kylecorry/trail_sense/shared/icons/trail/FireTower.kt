package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_fire_tower.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.FireTower: ImageVector
    get() {
        _FireTower?.let { return it }
        return ImageVector.Builder(
            name = "FireTower",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.6 7.1l-1.5 0.3 0.5 2.3-2.6 2.4-2.6-2.4 0.5-2.3-1.5-0.3L4.2 22h1.5l0.8-3.6 4.5-4.2 4.5 4.2 0.8 3.6h1.5L14.6 7.1zm-7.5 8.7l1-4.4 1.9 1.8-2.9 2.6zm5-2.6l1.9-1.8 0.9 4.4-2.8-2.6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M16 8V3l-5-2-5 2v5h10zM7 4h8v2H7V4z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _FireTower = it }
    }

private var _FireTower: ImageVector? = null
