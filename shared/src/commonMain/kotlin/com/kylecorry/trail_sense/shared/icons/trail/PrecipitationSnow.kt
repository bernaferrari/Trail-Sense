package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_precipitation_snow.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PrecipitationSnow: ImageVector
    get() {
        _PrecipitationSnow?.let { return it }
        return ImageVector.Builder(
            name = "PrecipitationSnow",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20.79 13.95l-2.33 0.62-2-1.13v-2.88l2-1.13 2.33 0.62 0.52-1.93-1.77-0.47L20 5.88l-1.93-0.52-0.62 2.33-2 1.13L13 7.38V5.12l1.71-1.71L13.29 2 12 3.29 10.71 2 9.29 3.41 11 5.12v2.26L8.5 8.82l-2-1.13-0.58-2.33L4 5.88l0.47 1.77L2.7 8.12l0.52 1.93 2.33-0.62 2 1.13v2.89l-2 1.13-2.33-0.62-0.52 1.93 1.77 0.47L4 18.12l1.93 0.52 0.62-2.33 2-1.13L11 16.62v2.26l-1.71 1.71L10.71 22 12 20.71 13.29 22l1.41-1.41-1.7-1.71v-2.26l2.5-1.45 2 1.13 0.62 2.33L20 18.12l-0.47-1.77 1.77-0.47-0.51-1.93M9.5 10.56L12 9.11l2.5 1.45v2.88L12 14.89l-2.5-1.45v-2.88z"),
            fill = SolidColor(Color(0xFFCCCCCC)),
        )
        }.build().also { _PrecipitationSnow = it }
    }

private var _PrecipitationSnow: ImageVector? = null
