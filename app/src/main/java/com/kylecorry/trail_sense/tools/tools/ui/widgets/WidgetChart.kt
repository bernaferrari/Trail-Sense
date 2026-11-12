package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSensePalette
import com.kylecorry.trail_sense.shared.views.trailSenseColorScheme
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme
import com.kylecorry.trail_sense.shared.views.palette
import com.kylecorry.trail_sense.shared.views.colorStyle

/**
 * A widget's chart as plain drawing calls. The same calls draw it live in the app (a Compose [Canvas]) and into the
 * image Glance shows on the home screen ([toBitmap], through Compose's [CanvasDrawScope]), so no View is involved
 * either way. [scheme] is the app's colours in the app, and the widget theme's on the home screen.
 */
fun interface WidgetChart {
    fun DrawScope.draw(scheme: ColorScheme)
}

/** [chart] drawn live, in the current theme's colours. */
@Composable
fun WidgetChartCanvas(chart: WidgetChart, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) { with(chart) { draw(scheme) } }
}

/** [this] drawn into a [widthPx] × [heightPx] bitmap for a home-screen widget, in [scheme]'s colours. */
fun WidgetChart.toBitmap(context: Context, widthPx: Int, heightPx: Int, scheme: ColorScheme): Bitmap {
    val width = widthPx.coerceAtLeast(1)
    val height = heightPx.coerceAtLeast(1)
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        Density(context),
        LayoutDirection.Ltr,
        Canvas(image),
        Size(width.toFloat(), height.toFloat())
    ) {
        with(this@toBitmap) { draw(scheme) }
    }
    return image.asAndroidBitmap()
}

/**
 * The app's colours (the person's palette and style, or the wallpaper's) for a widget theme: dark under the white
 * transparent theme's light text, light under the others.
 */
fun widgetColorScheme(context: Context, theme: WidgetTheme): ColorScheme {
    val prefs = UserPreferences(context)
    val dark = theme == WidgetTheme.TransparentWhite
    val wallpaper = if (prefs.palette == TrailSensePalette.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    return trailSenseColorScheme(prefs.palette, prefs.colorStyle, dark, wallpaperScheme = wallpaper)
}
