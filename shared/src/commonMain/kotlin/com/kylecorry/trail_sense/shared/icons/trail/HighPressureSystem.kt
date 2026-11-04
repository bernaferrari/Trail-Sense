package com.kylecorry.trail_sense.shared.icons.trail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The weather-map symbol as one glyph: a disc with its letter cut out, so it takes any tint. */
val TrailIcons.HighPressureSystem: ImageVector
    get() {
        _HighPressureSystem?.let { return it }
        return ImageVector.Builder(
            name = "HighPressureSystem",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes("M2 12a10 10 0 1 1 20 0 10 10 0 1 1-20 0z M6.625 6h3.093v4.573h4.564V6h3.093v12h-3.093v-5.088H9.718V18H6.625z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            )
        }.build().also { _HighPressureSystem = it }
    }

private var _HighPressureSystem: ImageVector? = null
