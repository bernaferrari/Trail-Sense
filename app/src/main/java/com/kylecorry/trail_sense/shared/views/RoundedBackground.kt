package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.annotation.ColorInt
import com.kylecorry.trail_sense.R

/** A filled rectangle with the app's rounded corners, for View backgrounds that clip their content. */
fun roundedRectangleBackground(context: Context, @ColorInt color: Int = Color.WHITE): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.resources.getDimension(R.dimen.rounded_corner_size)
        setColor(color)
    }
}
