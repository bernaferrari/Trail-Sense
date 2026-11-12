package com.kylecorry.trail_sense.shared.icons

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.roundToInt

/*
 * Trail Sense's icons are Compose vectors. Places on Android that need a bitmap or a platform icon (notifications,
 * home screen widgets and shortcuts) use the shared rasterizer, so there is one icon system and no drawable
 * resources.
 */

/** The vector as a bitmap, [width]×[height] px; tinted with [tint] (every painted pixel) when given. Cached. */
fun ImageVector.toBitmap(width: Int, height: Int = width, @ColorInt tint: Int? = null): Bitmap {
    return toImageBitmap(width, height, tint).asAndroidBitmap()
}

/** The vector at [sizeDp] for this screen's density. */
fun ImageVector.toBitmap(context: Context, sizeDp: Float = 24f, @ColorInt tint: Int? = null): Bitmap {
    val px = (sizeDp * context.resources.displayMetrics.density).roundToInt()
    val height = (px * viewportHeight / viewportWidth).roundToInt()
    return toBitmap(px, height, tint)
}

/** A platform icon (notifications, shortcuts) drawn from the vector. */
fun ImageVector.toIconCompat(context: Context, sizeDp: Float = 24f, @ColorInt tint: Int? = null): IconCompat =
    IconCompat.createWithBitmap(toBitmap(context, sizeDp, tint))
