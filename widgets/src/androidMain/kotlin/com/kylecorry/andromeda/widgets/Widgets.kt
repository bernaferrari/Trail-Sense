package com.kylecorry.andromeda.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

object Widgets {
    suspend fun requestUpdate(context: Context, widget: GlanceAppWidget) = widget.updateAll(context)

    suspend fun isWidgetInstantiated(context: Context, widget: GlanceAppWidget): Boolean =
        GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass).isNotEmpty()
}
