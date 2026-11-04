package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/signal_cellular_outline.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.SignalCellularOutline: ImageVector
    get() {
        _SignalCellularOutline?.let { return it }
        return ImageVector.Builder(
            name = "SignalCellularOutline",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M19.5 5.5v13h-2v-13h2m-7 5v8h-2v-8h2m-7 5v3h-2v-3h2M21 4h-5v16h5V4m-7 5H9v11h5V9m-7 5H2v6h5v-6z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _SignalCellularOutline = it }
    }

private var _SignalCellularOutline: ImageVector? = null
