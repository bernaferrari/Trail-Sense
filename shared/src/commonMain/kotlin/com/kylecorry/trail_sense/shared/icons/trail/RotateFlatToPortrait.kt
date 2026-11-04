package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_rotate_flat_to_portrait.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.RotateFlatToPortrait: ImageVector
    get() {
        _RotateFlatToPortrait?.let { return it }
        return ImageVector.Builder(
            name = "RotateFlatToPortrait",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 22.041h21.565V24H0zm7.154-8.031h1.113a3.905 3.905 0 0 1 3.896-3.665v1.673l2.248-2.254-0.078-0.039-2.17-2.17V9.23a5.02 5.02 135 0 0-5.009 4.78zM24 0v21.565h-1.959V0z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _RotateFlatToPortrait = it }
    }

private var _RotateFlatToPortrait: ImageVector? = null
