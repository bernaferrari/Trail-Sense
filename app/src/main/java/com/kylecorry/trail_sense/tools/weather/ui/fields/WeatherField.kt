package com.kylecorry.trail_sense.tools.weather.ui.fields

import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context

interface WeatherField {

    fun getListItem(context: Context): WeatherListItem?

}

data class WeatherListItem(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val iconTint: Int? = null,
    val trailingText: String? = null,
    val onClick: (() -> Unit)? = null
)
