package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.setPadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.settings.SettingsToolRegistration
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
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

    @SuppressLint("ClickableViewAccessibility")
    fun bind() {
        val selectedWidgets = prefs.toolWidgets
        val allWidgets = Tools.getTools(context)
            .flatMap { it.widgets }
            .filter { it.isEnabled(context) && it.canPlaceInApp }
            .sortedByDescending { it.size.ordinal }

        // Only show the selected widgets
        val widgets = allWidgets.filter { selectedWidgets.contains(it.id) }

        // 2 cells on a Pixel phone = 102dp
        val halfSummaryHeight = Resources.dp(context, 102f).toInt()
        val fullSummaryHeight = halfSummaryHeight * 2
        val summaryGap = Resources.dp(context, 4f).toInt()

        widgets.forEach { widget ->
            val layout = FrameLayout(context)

            val height = when (widget.size) {
                ToolSummarySize.Half -> halfSummaryHeight
                ToolSummarySize.Full -> fullSummaryHeight
                ToolSummarySize.TallFull -> (fullSummaryHeight * 1.5f).safeRoundToInt()
            }
            layout.setPadding(summaryGap)

            val widgetId = widget.id

            val updateFunction = {
                lifecycleOwner.lifecycleScope.launch {
                    val content = onDefault {
                        widget.widgetView.getContent(context, null)
                    }
                    onMain {
                        tryOrLog {
                            setWidgetContent(layout, content)
                            updateWidgetSnapshot(
                                widgetId,
                                renderWidget(layout, widget.size, height)
                            )
                        }
                    }
                }
                Unit
            }

            this.widgets.add(WidgetInstance(widget, layout, height, updateFunction = updateFunction))
            setWidgetContent(layout, widget.widgetView.getLoadingContent(context, null))
            updateWidgetSnapshot(widgetId, renderWidget(layout, widget.size, height))
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
        Tools.subscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)

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
        Tools.unsubscribe(SettingsToolRegistration.BROADCAST_UPDATE_IN_APP_WIDGET, this::onUpdate)
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

    private fun onUpdate(data: Bundle): Boolean {
        val widgetId = data.getString("widgetId") ?: return true
        val widget = widgets.find { it.widget.id == widgetId } ?: return true
        widget.updateFunction()
        return true
    }

    private fun setWidgetContent(layout: FrameLayout, content: ToolWidgetContent?) {
        val child = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            ToolWidgetComposeContent(content, Modifier.fillMaxSize())
        }
        layout.removeAllViews()
        layout.addView(child)
    }

    private fun renderWidget(
        layout: FrameLayout,
        size: ToolSummarySize,
        height: Int
    ): Bitmap {
        val width = when (size) {
            ToolSummarySize.Half -> Resources.dp(context, 172f).toInt()
            ToolSummarySize.Full, ToolSummarySize.TallFull -> Resources.dp(context, 352f).toInt()
        }
        return Views.renderViewAsBitmap(layout, width, height)
    }

    private fun updateWidgetSnapshot(widgetId: String, bitmap: Bitmap) {
        val snapshots = widgets.mapNotNull {
            val existing = it.snapshot
            val next = if (it.widget.id == widgetId) {
                ToolWidgetSnapshot(it.widget.id, it.widget.size, bitmap)
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
        val layout: FrameLayout,
        val height: Int,
        var snapshot: ToolWidgetSnapshot? = null,
        val updateFunction: () -> Unit = {}
    )
}

data class ToolWidgetSnapshot(
    val id: String,
    val size: ToolSummarySize,
    val bitmap: Bitmap
)
