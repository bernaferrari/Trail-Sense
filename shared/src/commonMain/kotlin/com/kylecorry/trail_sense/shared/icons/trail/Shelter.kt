package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/shelter.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Shelter: ImageVector
    get() {
        _Shelter?.let { return it }
        return ImageVector.Builder(
            name = "Shelter",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 30.0f,
            viewportHeight = 30.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M30,4l0,-3l-30,13l0,3l3,-1.3l0,12.3l27,0l0,-3l-24,0l0,-10.6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Shelter = it }
    }

private var _Shelter: ImageVector? = null
