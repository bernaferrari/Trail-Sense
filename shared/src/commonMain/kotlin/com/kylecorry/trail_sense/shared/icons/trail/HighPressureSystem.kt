package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_high_pressure_system.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.HighPressureSystem: ImageVector
    get() {
        _HighPressureSystem?.let { return it }
        return ImageVector.Builder(
            name = "HighPressureSystem",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 12a12 12 0 1 1 24 0 12 12 0 1 1-24 0"),
            fill = SolidColor(Color(0xFF304FFE)),
        )
        addPath(
            pathData = addPathNodes("M1.529 12a10.471 10.471 0 1 1 20.942 0 10.471 10.471 0 1 1-20.942 0"),
            stroke = SolidColor(Color(0xFFFFFFFF)),
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        addPath(
            pathData = addPathNodes("M6.625 6h3.093v4.573h4.564V6h3.093v12h-3.093v-5.088H9.718V18H6.625z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _HighPressureSystem = it }
    }

private var _HighPressureSystem: ImageVector? = null
