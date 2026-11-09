package com.kylecorry.trail_sense.shared.views.legacy

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt

/** Tints the image with [color] (SRC_IN), or clears the tint when [color] is null. */
fun ImageView.setImageColor(@ColorInt color: Int?) {
    if (color == null) {
        clearColorFilter()
        return
    }
    colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
}

/** Tints the drawable with [color] (SRC_IN), or clears the tint when [color] is null. */
fun Drawable.setImageColor(@ColorInt color: Int?) {
    if (color == null) {
        clearColorFilter()
        return
    }
    colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
}

/** Tints every compound drawable of the text view with [color], or clears the tint when null. */
fun TextView.setCompoundDrawablesColor(@ColorInt color: Int?) {
    compoundDrawables.forEach { it?.setImageColor(color) }
}
