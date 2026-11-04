package com.kylecorry.trail_sense.shared.icons.trail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The weather-map symbol as one glyph: a disc with its letter cut out, so it takes any tint. */
val TrailIcons.LowPressureSystem: ImageVector
    get() {
        _LowPressureSystem?.let { return it }
        return ImageVector.Builder(
            name = "LowPressureSystem",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes("M2 12a10 10 0 1 1 20 0 10 10 0 1 1-20 0z M7.613 6h3.184v9.661h5.59V18H7.613z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd
            )
        }.build().also { _LowPressureSystem = it }
    }

private var _LowPressureSystem: ImageVector? = null
