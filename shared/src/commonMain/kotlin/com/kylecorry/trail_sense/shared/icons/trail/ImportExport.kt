package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_import_export.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ImportExport: ImageVector
    get() {
        _ImportExport?.let { return it }
        return ImageVector.Builder(
            name = "ImportExport",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9 3L5 6.99h3V14h2V6.99h3L9 3zm7 14.01V10h-2v7.01h-3L15 21l4-3.99h-3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _ImportExport = it }
    }

private var _ImportExport: ImageVector? = null
