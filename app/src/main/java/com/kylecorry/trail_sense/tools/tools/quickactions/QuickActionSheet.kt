@file:Suppress("FunctionName")

package com.kylecorry.trail_sense.tools.tools.quickactions

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBinder

@SuppressLint("ClickableViewAccessibility")
class QuickActionSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val content: ComposeView
    private val widgetsContainer: ScrollView
    private val widgets: FlexboxLayout
    private val quickActionState = MainActivityQuickActionState()
    private var selectedTab by mutableIntStateOf(0)

    private var quickActionBinder: MainActivityQuickActionBinder? = null
    private var widgetBinder: ToolWidgetViewBinder? = null
    private var backCallback: OnBackPressedCallback? = null

    init {
        if (id == View.NO_ID) {
            id = R.id.quick_actions_sheet
        }

        val padding = Resources.dp(context, 8f).toInt()
        val surfaceColor = Resources.getAndroidColorAttr(
            context,
            com.google.android.material.R.attr.colorSurfaceContainer
        )
        setBackgroundColor(surfaceColor)
        setPadding(padding, padding, padding, padding)

        isClickable = true
        isFocusable = true
        setOnTouchListener { _, _ -> true }

        content = ComposeView(context).apply {
            id = R.id.tabs
            setContent {
                TrailSenseTheme {
                    QuickActionSheetContent(
                        selectedTab = selectedTab,
                        state = quickActionState,
                        onTabSelected = {
                            if (it == 0) {
                                showQuickActions()
                            } else {
                                showWidgets()
                            }
                        },
                        onClose = {
                            close()
                        }
                    )
                }
            }
        }
        addView(
            content,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
            }
        )

        widgetsContainer = object : ScrollView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val maxHeight = Resources.dp(context, 350f).toInt()
                super.onMeasure(
                    widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                )
            }
        }.apply {
            id = R.id.widgets_container
            isNestedScrollingEnabled = true
        }
        addView(
            widgetsContainer,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = R.id.tabs
            }
        )

        widgets = FlexboxLayout(context).apply {
            id = R.id.widgets
            alignItems = AlignItems.CENTER
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
            setPadding(
                0,
                Resources.dp(context, 16f).toInt(),
                0,
                Resources.dp(context, 16f).toInt()
            )
        }
        widgetsContainer.addView(
            widgets,
            android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = Resources.dp(context, 16f).toInt()
                rightMargin = Resources.dp(context, 16f).toInt()
            }
        )
    }

    fun close() {
        quickActionState.clear()
        isVisible = false
        widgetBinder?.unbind()
        widgetBinder = null
        backCallback?.remove()
        backCallback = null
    }

    fun show(activity: MainActivity, tab: Int = 0) {
        if (tab == 0) {
            showQuickActions()
        } else {
            showWidgets()
        }

        if (isOpen()) {
            return
        }

        val fragment = activity.getFragment()

        if (fragment == null) {
            Alerts.toast(context, context.getString(R.string.quick_actions_are_unavailable))
            return
        }

        widgetBinder?.unbind()
        quickActionBinder = MainActivityQuickActionBinder(
            fragment,
            quickActionState
        )
        widgetBinder = ToolWidgetViewBinder(fragment, widgets)
        quickActionBinder?.bind()
        widgetBinder?.bind()
        isVisible = true

        backCallback?.remove()
        backCallback = activity.onBackPressedDispatcher.addCallback(activity, true) {
            close()
        }
    }

    fun isOpen(): Boolean {
        return isVisible
    }

    private fun showQuickActions() {
        selectedTab = 0
        widgetsContainer.isVisible = false
    }

    private fun showWidgets() {
        selectedTab = 1
        widgetsContainer.isVisible = true
    }

}

@Composable
private fun QuickActionSheetContent(
    selectedTab: Int,
    state: MainActivityQuickActionState,
    onTabSelected: (Int) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
            androidx.compose.foundation.layout.Box(modifier = Modifier.padding(top = 1.dp))
        }
        QuickActionButtonRow(state.selected)
    }
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
