package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tide_low.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TideLow: ImageVector
    get() {
        _TideLow?.let { return it }
        return ImageVector.Builder(
            name = "TideLow",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 12a12 12 0 1 1 24 0 12 12 0 1 1-24 0"),
            fill = SolidColor(Color(0xFF9E9E9E)),
            fillAlpha = 0.2f,
        )
        addPath(
            pathData = addPathNodes("M6.045 16.504l-0.021 0.002-0.007-0.001c-1.209-0.04-1.738 2.124-2.947 2.117-0.852-0.005-1.349-1.086-1.998-1.701A12 12 0 0 0 12 24a12 12 135 0 0 11.062-7.354c-0.847 0.475-1.359 1.976-2.363 1.976-1.19-0.001-1.683-2.116-2.873-2.118-0.036 0-0.074 0.006-0.111 0.015-0.031-0.008-0.061-0.013-0.091-0.014-1.21-0.04-1.739 2.124-2.948 2.117-1.19-0.008-1.684-2.118-2.874-2.118-1.193-0.001-1.691 2.118-2.883 2.118-1.19-0.001-1.684-2.116-2.874-2.118z"),
            fill = SolidColor(Color(0xFFA0CAFF)),
        )
        }.build().also { _TideLow = it }
    }

private var _TideLow: ImageVector? = null
