package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_restrooms.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Restrooms: ImageVector
    get() {
        _Restrooms?.let { return it }
        return ImageVector.Builder(
            name = "Restrooms",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 20.0f,
            viewportHeight = 20.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10 1.8h1.8v16.4H10zm9.1 4.6V5.5h-4.6v0.9h-0.9v5.4h0.9V7.3h1v10.9h0.9v-5.5h0.9v5.5h0.9V7.3h0.9v4.5H20V6.4zm-11.8 0L5.5 5.5H2.7L0.9 6.4v0.9L0 10.9h0.9l0.9-3.6h0.9v0.9l-1.8 5.4h1.8v4.6h0.9v-4.6h0.9v4.6h1v-4.6h1.8L5.5 8.2V7.3h0.9l0.9 3.6h0.9L7.3 7.3zM2.7 3.2a1.4 1.4 0 1 1 2.8 0 1.4 1.4 0 1 1-2.8 0m12.7 0a1.4 1.4 0 1 1 2.8 0 1.4 1.4 0 1 1-2.8 0"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Restrooms = it }
    }

private var _Restrooms: ImageVector? = null
