@file:Suppress("FunctionName")

package com.kylecorry.trail_sense.tools.tools.quickactions

import android.annotation.SuppressLint
import android.content.Context
import android.view.View.MeasureSpec
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBinder

class QuickActionSheetState(private val context: Context) {

    private val quickActionState = MainActivityQuickActionState()
    private var selectedTab by mutableIntStateOf(0)
    private var open by mutableStateOf(false)
    private var activity: MainActivity? = null
    private var widgets: FlexboxLayout? = null

    private var quickActionBinder: MainActivityQuickActionBinder? = null
    private var widgetBinder: ToolWidgetViewBinder? = null
    private var backCallback: OnBackPressedCallback? = null

    fun close() {
        quickActionState.clear()
        open = false
        widgetBinder?.unbind()
        widgetBinder = null
        widgets = null
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

        val fragment = activity.getFragment()

        if (fragment == null) {
            Alerts.toast(context, context.getString(R.string.quick_actions_are_unavailable))
            return
        }

        widgetBinder?.unbind()
        widgetBinder = null
        quickActionBinder = MainActivityQuickActionBinder(
            fragment,
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
        val fragment = activity?.getFragment() ?: return
        val widgets = widgets ?: return
        if (widgetBinder != null) {
            return
        }
        widgetBinder = ToolWidgetViewBinder(
            fragment.requireContext(),
            fragment.viewLifecycleOwner,
            widgets
        )
        widgetBinder?.bind()
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!open) {
            return
        }

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
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
                    WidgetsContent { widgets ->
                        this.widgets = widgets
                        bindWidgetsIfReady()
                    }
                }
            )
        }
    }
}

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
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.weight(1f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    text = { Text(stringResource(R.string.quick_actions)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    text = { Text(stringResource(R.string.widgets)) }
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedTab == 0) {
            QuickActionsContent(state)
        } else {
            widgetsContent()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionsContent(state: MainActivityQuickActionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        QuickActionButtonRow(state.recommended)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Box(modifier = Modifier.padding(top = 1.dp))
        }
        QuickActionButtonRow(state.selected)
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun WidgetsContent(onWidgetsCreated: (FlexboxLayout) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            object : ScrollView(context) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    val maxHeight = Resources.dp(context, 350f).toInt()
                    super.onMeasure(
                        widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                    )
                }
            }.apply {
                isNestedScrollingEnabled = true
                val widgets = FlexboxLayout(context).apply {
                    alignItems = AlignItems.CENTER
                    flexWrap = FlexWrap.WRAP
                    justifyContent = JustifyContent.FLEX_START
                    setPadding(
                        Resources.dp(context, 16f).toInt(),
                        Resources.dp(context, 16f).toInt(),
                        Resources.dp(context, 16f).toInt(),
                        Resources.dp(context, 16f).toInt()
                    )
                }
                addView(
                    widgets,
                    android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                onWidgetsCreated(widgets)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionButtonRow(buttons: List<ComposeQuickActionButtonView>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center
    ) {
        buttons.forEach { button ->
            Surface(
                modifier = Modifier.padding(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                button.Content()
            }
        }
    }
}
