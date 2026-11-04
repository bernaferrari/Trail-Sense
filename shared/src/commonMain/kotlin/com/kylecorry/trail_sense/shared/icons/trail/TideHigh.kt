package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tide_high.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TideHigh: ImageVector
    get() {
        _TideHigh?.let { return it }
        return ImageVector.Builder(
            name = "TideHigh",
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
            pathData = addPathNodes("M6.349 2.537L6.328 2.539 6.321 2.538c-1.209-0.04-1.738 2.124-2.947 2.117-0.247-0.002-0.462-0.097-0.661-0.241A12 12 135 0 0 0 12a12 12 45 0 0 12 12 12 12 45 0 0 12-12 12 12 45 0 0-2.583-7.435c-0.129 0.057-0.267 0.09-0.415 0.09-1.19-0.001-1.684-2.116-2.874-2.118-0.036 0-0.074 0.006-0.111 0.015-0.031-0.008-0.061-0.013-0.091-0.014-1.209-0.04-1.738 2.124-2.947 2.117-1.19-0.007-1.684-2.117-2.874-2.118-1.193-0.001-1.69 2.119-2.883 2.118-1.19-0.001-1.684-2.116-2.874-2.118z"),
            fill = SolidColor(Color(0xFFA0CAFF)),
        )
        }.build().also { _TideHigh = it }
    }

private var _TideHigh: ImageVector? = null
