package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_update.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Update: ImageVector
    get() {
        _Update?.let { return it }
        return ImageVector.Builder(
            name = "Update",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M21 10.12h-6.78l2.74-2.82c-2.73-2.7-7.15-2.8-9.88-0.1-2.73 2.71-2.73 7.08 0 9.79s7.15 2.71 9.88 0C18.32 15.65 19 14.08 19 12.1h2c0 1.98-0.88 4.55-2.64 6.29-3.51 3.48-9.21 3.48-12.72 0-3.5-3.47-3.53-9.11-0.02-12.58s9.14-3.47 12.65 0L21 3v7.12zM12.5 8v4.25l3.5 2.08-0.72 1.21L11 13V8h1.5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Update = it }
    }

private var _Update: ImageVector? = null
