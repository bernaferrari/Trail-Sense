package com.kylecorry.trail_sense.tools.tools.quickactions

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import android.widget.ScrollView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.JustifyContent
import com.google.android.material.tabs.TabLayout
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetViewBinder

@SuppressLint("ClickableViewAccessibility")
class QuickActionSheet @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val closeButton: View
    private val tabs: TabLayout
    private val quickActionsContainer: ConstraintLayout
    private val recommendedQuickActions: FlexboxLayout
    private val quickActions: FlexboxLayout
    private val widgetsContainer: ScrollView
    private val widgets: FlexboxLayout

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

        tabs = TabLayout(context).apply {
            id = R.id.tabs
            setBackgroundColor(surfaceColor)
            elevation = 0f
            tabMode = TabLayout.MODE_AUTO
        }
        addView(
            tabs,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                endToStart = R.id.close_button
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
            }
        )

        closeButton = ComposeView(context).apply {
            id = R.id.close_button
            setContent {
                TrailSenseTheme {
                    IconButton(onClick = { close() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        addView(
            closeButton,
            LayoutParams(
                Resources.dp(context, 32f).toInt(),
                Resources.dp(context, 32f).toInt()
            ).apply {
                bottomToBottom = R.id.tabs
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
            }
        )

        quickActionsContainer = ConstraintLayout(context).apply {
            id = R.id.quick_actions_container
        }
        addView(
            quickActionsContainer,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Resources.dp(context, 16f).toInt()
                topToBottom = R.id.tabs
            }
        )

        recommendedQuickActions = FlexboxLayout(context).apply {
            id = R.id.recommended_quick_actions
            alignItems = AlignItems.CENTER
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.CENTER
        }
        quickActionsContainer.addView(
            recommendedQuickActions,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
            }
        )

        val quickActionDividerId = View.generateViewId()
        quickActionsContainer.addView(
            ComposeView(context).apply {
                id = quickActionDividerId
                setContent {
                    TrailSenseTheme {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            },
            LayoutParams(
                0,
                Resources.dp(context, 1f).toInt()
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToBottom = R.id.recommended_quick_actions
                topMargin = Resources.dp(context, 8f).toInt()
                bottomMargin = Resources.dp(context, 8f).toInt()
            }
        )

        quickActions = FlexboxLayout(context).apply {
            id = R.id.quick_actions
            alignItems = AlignItems.CENTER
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.CENTER
        }
        quickActionsContainer.addView(
            quickActions,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToBottom = quickActionDividerId
                bottomToBottom = LayoutParams.PARENT_ID
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
        tabs.addTab(
            tabs.newTab().also { it.text = context.getString(R.string.quick_actions) })
        tabs.addTab(
            tabs.newTab().also { it.text = context.getString(R.string.widgets) })

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab == null) {
                    return
                }
                when (tab.position) {
                    0 -> {
                        showQuickActions()
                    }

                    1 -> {
                        showWidgets()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    fun close() {
        quickActions.removeAllViews()
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
            recommendedQuickActions,
            quickActions
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
        if (tabs.selectedTabPosition != 0) {
            tabs.selectTab(tabs.getTabAt(0))
        }
        quickActionsContainer.isVisible = true
        widgetsContainer.isVisible = false
    }

    private fun showWidgets() {
        if (tabs.selectedTabPosition != 1) {
            tabs.selectTab(tabs.getTabAt(1))
        }
        quickActionsContainer.isVisible = false
        widgetsContainer.isVisible = true
    }

}
