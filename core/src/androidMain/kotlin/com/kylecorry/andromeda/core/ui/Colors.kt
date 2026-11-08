package com.kylecorry.andromeda.core.ui

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable

fun Colors.setImageColor(drawable: Drawable, color: Int?) {
    if (color == null) drawable.clearColorFilter()
    else drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
}
