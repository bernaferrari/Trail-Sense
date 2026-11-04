package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_beacon_group.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.BeaconGroup: ImageVector
    get() {
        _BeaconGroup?.let { return it }
        return ImageVector.Builder(
            name = "BeaconGroup",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M2.4 2.4c-1.32 0-2.388 1.08-2.388 2.4L0 19.2c0 1.32 1.08 2.4 2.4 2.4h19.2c1.32 0 2.4-1.08 2.4-2.4v-12c0-1.32-1.08-2.4-2.4-2.4H12L9.6 2.4H2.4zM12 6c2.322 0 4.2 1.877 4.2 4.2 0 3.15-4.2 7.8-4.2 7.8s-4.2-4.65-4.2-7.8C7.8 7.876 9.679 6 12 6zm0 2.7c-0.828 0-1.5 0.671-1.5 1.5 0 0.827 0.672 1.5 1.5 1.5s1.5-0.673 1.5-1.5c0-0.829-0.672-1.5-1.5-1.5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _BeaconGroup = it }
    }

private var _BeaconGroup: ImageVector? = null
