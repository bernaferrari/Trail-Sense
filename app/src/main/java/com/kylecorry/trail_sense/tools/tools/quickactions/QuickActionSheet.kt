@file:Suppress("FunctionName")

package com.kylecorry.trail_sense.tools.tools.quickactions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BottomSheetDefaults
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetComposeContent
import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolSummarySize
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBinder
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetSnapshot

class QuickActionSheetState(private val context: Context) {

    private val quickActionState = MainActivityQuickActionState()
    private var selectedTab by mutableIntStateOf(0)
    private var open by mutableStateOf(false)
    private var activity: MainActivity? = null
    private var widgets by mutableStateOf(emptyList<ToolWidgetSnapshot>())

    private var quickActionBinder: MainActivityQuickActionBinder? = null
    private var widgetBinder: ToolWidgetViewBinder? = null
    private var backCallback: OnBackPressedCallback? = null

    fun close() {
        quickActionState.clear()
        open = false
        widgetBinder?.unbind()
        widgetBinder = null
        widgets = emptyList()
        quickActionBinder = null
        backCallback?.remove()
        backCallback = null
    }

    fun show(activity: MainActivity, tab: Int = 0) {
        this.activity = activity
        selectedTab = tab

        if (isOpen()) {
            return
        }

        val host = activity.getActiveDestinationHost()

        if (host == null) {
            Alerts.toast(context, context.getString(R.string.quick_actions_are_unavailable))
            return
        }

        widgetBinder?.unbind()
        widgetBinder = null
        quickActionBinder = MainActivityQuickActionBinder(
            host,
            quickActionState
        )
        quickActionBinder?.bind()
        bindWidgetsIfReady()
        open = true

        backCallback?.remove()
        backCallback = activity.onBackPressedDispatcher.addCallback(activity, true) {
            close()
        }
    }

    fun isOpen(): Boolean {
        return open
    }

    private fun bindWidgetsIfReady() {
        val host = activity?.getActiveDestinationHost() ?: return
        if (widgetBinder != null) {
            return
        }
        widgetBinder = ToolWidgetViewBinder(
            host.requireContext(),
            host.lifecycleOwner
        ) {
            widgets = it
        }
        widgetBinder?.bind()
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!open) {
            return
        }

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 8.dp
        ) {
            QuickActionSheetContent(
                selectedTab = selectedTab,
                state = quickActionState,
                onTabSelected = {
                    selectedTab = it
                },
                onClose = { close() },
                widgetsContent = {
                    WidgetsContent(widgets)
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionSheetContent(
    selectedTab: Int,
    state: MainActivityQuickActionState,
    onTabSelected: (Int) -> Unit,
    onClose: () -> Unit,
    widgetsContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, bottom = 16.dp)
    ) {
        BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseConnectedChoice(
                options = listOf(
                    TrailSenseChoice(0, stringResource(R.string.quick_actions)),
                    TrailSenseChoice(1, stringResource(R.string.widgets))
                ),
                selected = selectedTab,
                onSelect = onTabSelected,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "quickActionTab",
            modifier = Modifier.padding(end = 8.dp)
        ) { tab ->
            if (tab == 0) QuickActionsContent(state) else widgetsContent()
        }
    }
}

/** This tab's own quick actions, then the ones you chose: two connected cards of round toggles. */
@Composable
private fun QuickActionsContent(state: MainActivityQuickActionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val groups = listOf(state.recommended, state.selected).filter { it.isNotEmpty() }
        groups.forEachIndexed { index, buttons ->
            // Inside a sheet, groups lift one tone above it (the family rule).
            TrailSenseGroupCard(
                TrailSenseGroupPosition.at(index, groups.size),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                QuickActionButtonRow(buttons)
            }
        }
    }
}

@Composable
private fun WidgetsContent(widgets: List<ToolWidgetSnapshot>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Top
    ) {
        widgets.forEach { widget ->
            val modifier = when (widget.size) {
                ToolSummarySize.Half -> Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(172f / 102f)
                ToolSummarySize.Full -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(352f / 204f)
                ToolSummarySize.TallFull -> Modifier
                    .fillMaxWidth()
                    .aspectRatio(352f / 306f)
            }
            ToolWidgetComposeContent(widget.content, modifier.padding(4.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionButtonRow(buttons: List<ComposeQuickActionButtonView>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        buttons.forEach { button -> button.Content() }
    }
}
