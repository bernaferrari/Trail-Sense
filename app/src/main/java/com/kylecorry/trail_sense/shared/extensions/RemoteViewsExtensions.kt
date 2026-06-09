package com.kylecorry.trail_sense.shared.extensions

import android.content.Context
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes

fun RemoteViews.setImageViewResourceAsIcon(
    context: Context,
    @IdRes viewId: Int,
    @DrawableRes drawableId: Int
) {
    setImageViewIcon(viewId, Icon.createWithResource(context, drawableId))
}
