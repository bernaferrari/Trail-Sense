package com.kylecorry.andromeda.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

/** Creates a shared widget manager backed by an Android Glance widget. */
public fun widgetManager(context: Context, widget: GlanceAppWidget): WidgetManager {
    val app = context.applicationContext
    return WidgetManager(
        updateWidget = { widget.updateAll(app) },
        installedWidget = { GlanceAppWidgetManager(app).getGlanceIds(widget.javaClass).isNotEmpty() },
    )
}
