package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch

class ToolWidgetViewBinder(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onWidgetsChanged: (List<ToolWidgetSnapshot>) -> Unit
) {

    private val widgets = mutableListOf<WidgetInstance>()
    private val prefs by lazy { UserPreferences(context) }

    private val lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_DESTROY -> onDestroy()
            else -> {}
        }
    }

    fun bind() {
        val selectedWidgets = prefs.toolWidgets
        val allWidgets = Tools.getTools(context)
            .flatMap { it.widgets }
            .filter { it.isEnabled(context) && it.canPlaceInApp }
            .sortedByDescending { it.size.ordinal }

        // Only show the selected widgets; every widget until the user chooses
        val widgets = selectedWidgets?.let { selected -> allWidgets.filter { it.id in selected } } ?: allWidgets

        widgets.forEach { widget ->
            val widgetId = widget.id

            val updateFunction = {
                lifecycleOwner.lifecycleScope.launch {
                    val content = onDefault {
                        widget.widgetView.getContent(context, null)
                    }
                    onMain {
                        tryOrLog {
                            updateWidgetSnapshot(widgetId, content)
                        }
                    }
                }
                Unit
            }

            this.widgets.add(WidgetInstance(widget, updateFunction = updateFunction))
            updateWidgetSnapshot(widgetId, widget.widgetView.getLoadingContent(context, null))
        }

        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_CREATE,
                it.updateFunction
            )
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleEventObserver)
    }

    fun unbind() {
        lifecycleOwner.lifecycle.removeObserver(lifecycleEventObserver)
        onPause()
        onDestroy()
        onWidgetsChanged(emptyList())
    }

    private fun onResume() {
        ToolBroadcasts.subscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)

        // TODO: Get a GPS / elevation reading

        // Update all widgets
        widgets.forEach { it.updateFunction() }
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_RESUME,
                it.updateFunction
            )
        }
    }

    private fun onPause() {
        ToolBroadcasts.unsubscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_PAUSE,
                it.updateFunction
            )
        }
    }

    private fun onDestroy() {
        this.widgets.forEach {
            it.widget.widgetView.onInAppEvent(
                context,
                Lifecycle.Event.ON_DESTROY,
                it.updateFunction
            )
        }
        this.widgets.clear()
        onWidgetsChanged(emptyList())
    }

    private fun onUpdate(data: EventData): Boolean {
        val widgetId = data.getString("widgetId") ?: return true
        val widget = widgets.find { it.widget.id == widgetId } ?: return true
        widget.updateFunction()
        return true
    }

    // The sheet draws each widget's content live; rendering a detached ComposeView into a bitmap crashed (a Compose
    // view can't compose before it is attached to a window).
    private fun updateWidgetSnapshot(widgetId: String, content: ToolWidgetContent?) {
        val snapshots = widgets.mapNotNull {
            val existing = it.snapshot
            val next = if (it.widget.id == widgetId) {
                ToolWidgetSnapshot(it.widget.id, it.widget.size, content)
            } else {
                existing
            }
            it.snapshot = next
            next
        }
        onWidgetsChanged(snapshots)
    }

    private data class WidgetInstance(
        val widget: ToolWidget,
        var snapshot: ToolWidgetSnapshot? = null,
        val updateFunction: () -> Unit = {}
    )
}

data class ToolWidgetSnapshot(
    val id: String,
    val size: ToolSummarySize,
    val content: ToolWidgetContent?
)
