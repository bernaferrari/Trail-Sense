package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tide_half.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TideHalf: ImageVector
    get() {
        _TideHalf?.let { return it }
        return ImageVector.Builder(
            name = "TideHalf",
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
            pathData = addPathNodes("M0.196 10.128L0.162 10.13A12 12 135 0 0 0 12a12 12 45 0 0 12 12 12 12 45 0 0 12-12 12 12 135 0 0-0.15-1.834c-0.085-0.024-0.174-0.038-0.268-0.038-1.192-0.002-1.69 2.118-2.883 2.118-1.19-0.002-1.683-2.117-2.873-2.118-0.036 0-0.074 0.006-0.111 0.015-0.031-0.008-0.061-0.013-0.091-0.014-1.21-0.04-1.739 2.124-2.948 2.117-1.19-0.008-1.684-2.118-2.874-2.118-1.193-0.002-1.691 2.118-2.883 2.118-1.19-0.002-1.684-2.117-2.874-2.118L6.024 10.13l-0.007-0.001c-1.209-0.04-1.738 2.124-2.947 2.117-1.19-0.007-1.684-2.117-2.874-2.118z"),
            fill = SolidColor(Color(0xFFA0CAFF)),
        )
        }.build().also { _TideHalf = it }
    }

private var _TideHalf: ImageVector? = null
