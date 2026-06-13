package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetGlanceContent

abstract class AppWidgetBase(private val widgetId: String) : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TrailSenseGlanceWidget(widgetId)

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val widget = Tools.getWidget(context, widgetId)!!
        appWidgetIds.forEach {
            val widgetPrefs = WidgetPreferences(context, widget, it)
            widgetPrefs.clear()
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        suspend fun forceUpdate(context: Context, widgetProvider: AppWidgetBase) {
            widgetProvider.glanceAppWidget.updateAll(context)
        }
    }
}

private class TrailSenseGlanceWidget(private val widgetId: String) : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val widget = Tools.getWidget(context, widgetId)
        val preferences = widget?.let { WidgetPreferences(context, it, appWidgetId) }
        val theme = preferences?.getTheme() ?: WidgetTheme.System
        val content = onDefault {
            widget?.widgetView?.getContent(context, preferences)
        }

        provideContent {
            ToolWidgetGlanceContent(
                content = content,
                theme = theme
            )
        }
    }
}
