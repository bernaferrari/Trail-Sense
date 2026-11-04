package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_open_book.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.OpenBook: ImageVector
    get() {
        _OpenBook?.let { return it }
        return ImageVector.Builder(
            name = "OpenBook",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M21 5c-1.11-0.35-2.33-0.5-3.5-0.5-1.95 0-4.05 0.4-5.5 1.5-1.45-1.1-3.55-1.5-5.5-1.5S2.45 4.9 1 6v14.65c0 0.25 0.25 0.5 0.5 0.5 0.1 0 0.15-0.05 0.25-0.05C3.1 20.45 5.05 20 6.5 20c1.95 0 4.05 0.4 5.5 1.5 1.35-0.85 3.8-1.5 5.5-1.5 1.65 0 3.35 0.3 4.75 1.05 0.1 0.05 0.15 0.05 0.25 0.05 0.25 0 0.5-0.25 0.5-0.5V6c-0.6-0.45-1.25-0.75-2-1zm0 13.5c-1.1-0.35-2.3-0.5-3.5-0.5-1.7 0-4.15 0.65-5.5 1.5V8c1.35-0.85 3.8-1.5 5.5-1.5 1.2 0 2.4 0.15 3.5 0.5v11.5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M17.5 10.5c0.88 0 1.73 0.09 2.5 0.26V9.24C19.21 9.09 18.36 9 17.5 9c-1.7 0-3.24 0.29-4.5 0.83v1.66c1.13-0.64 2.7-0.99 4.5-0.99zM13 12.49v1.66c1.13-0.64 2.7-0.99 4.5-0.99 0.88 0 1.73 0.09 2.5 0.26V11.9c-0.79-0.15-1.64-0.24-2.5-0.24-1.7 0-3.24 0.3-4.5 0.83zm4.5 1.84c-1.7 0-3.24 0.29-4.5 0.83v1.66c1.13-0.64 2.7-0.99 4.5-0.99 0.88 0 1.73 0.09 2.5 0.26v-1.52c-0.79-0.16-1.64-0.24-2.5-0.24z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _OpenBook = it }
    }

private var _OpenBook: ImageVector? = null
