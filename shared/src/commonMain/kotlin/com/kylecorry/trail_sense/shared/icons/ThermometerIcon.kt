package com.kylecorry.trail_sense.shared.icons

// Generated from Google Material Symbols Rounded. Do not edit by hand.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
internal val FilledThermometer: ImageVector
  get() {
    if (_FilledThermometer != null) {
      return _FilledThermometer!!
    }
    _FilledThermometer =
      ImageVector.Builder(
          name = "thermometer",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(12f, 21f)
            quadTo(9.93f, 21f, 8.46f, 19.54f)
            reflectiveQuadTo(7f, 16f)
            quadTo(7f, 14.8f, 7.53f, 13.76f)
            reflectiveQuadTo(9f, 12f)
            verticalLineTo(6f)
            quadTo(9f, 4.75f, 9.88f, 3.88f)
            reflectiveQuadTo(12f, 3f)
            reflectiveQuadToRelative(2.13f, 0.88f)
            reflectiveQuadTo(15f, 6f)
            verticalLineToRelative(6f)
            quadToRelative(0.95f, 0.72f, 1.48f, 1.76f)
            reflectiveQuadTo(17f, 16f)
            quadToRelative(0f, 2.07f, -1.46f, 3.54f)
            reflectiveQuadTo(12f, 21f)
            close()
            moveTo(11f, 10f)
            horizontalLineToRelative(2f)
            verticalLineTo(6f)
            quadTo(13f, 5.57f, 12.71f, 5.29f)
            reflectiveQuadTo(12f, 5f)
            reflectiveQuadTo(11.29f, 5.29f)
            reflectiveQuadTo(11f, 6f)
            verticalLineToRelative(4f)
            close()
          }
        }
        .build()
    return _FilledThermometer!!
  }

internal var _FilledThermometer: ImageVector? = null
