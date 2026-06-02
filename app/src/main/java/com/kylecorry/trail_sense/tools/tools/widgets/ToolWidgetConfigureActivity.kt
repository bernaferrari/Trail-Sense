package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.trail_sense.shared.views.Toolbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ToolWidgetConfigureActivity : AndromedaActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var configureViews: ToolWidgetConfigureViews

    override fun onCreate(savedInstanceState: Bundle?) {
        setColorTheme(ColorTheme.System, true)
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        configureViews = createToolWidgetConfigureViews(this)
        setContentView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    TrailSenseTheme {
                        ToolWidgetConfigureHost(
                            views = configureViews,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        )

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(configureViews.root) { v, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
                leftMargin = insets.left
                rightMargin = insets.right
            }
            windowInsets
        }

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val widget = Tools.getWidgetFromAppWidgetId(this, appWidgetId)
        if (widget == null) {
            finish()
            return
        }

        val prefs = WidgetPreferences(this, widget, appWidgetId)
        val title = configureViews.title
        val spinner = configureViews.spinner
        val save = configureViews.saveButton

        title.title.text = widget.name
        spinner.setHint(getString(R.string.pref_theme_title))

        val items = listOf(
            WidgetTheme.System to getString(R.string.theme_system),
            WidgetTheme.TransparentBlack to getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_black_text)
            ),
            WidgetTheme.TransparentWhite to getString(
                R.string.theme_transparent_type,
                getString(R.string.widget_theme_white_text)
            )
        )

        spinner.setItems(items.map { it.second })
        spinner.setSelection(items.indexOfFirst { it.first == prefs.getTheme() })

        save.setOnClickListener {
            val theme = items.getOrElse(spinner.selectedItemPosition) { items[0] }.first
            prefs.setTheme(theme)
            inBackground {
                val widgetProvider =
                    widget.widgetClass.getConstructor().newInstance() as? AppWidgetBase
                widgetProvider?.let {
                    AppWidgetBase.forceUpdate(
                        this@ToolWidgetConfigureActivity,
                        it,
                        appWidgetId
                    )
                }

                // Report the result
                val resultValue =
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
    }
}

private data class ToolWidgetConfigureViews(
    val root: View,
    val title: Toolbar,
    val spinner: MaterialSpinnerView,
    val saveButton: Button
)

@Composable
private fun ToolWidgetConfigureHost(
    views: ToolWidgetConfigureViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createToolWidgetConfigureViews(activity: ToolWidgetConfigureActivity): ToolWidgetConfigureViews {
    val root = LinearLayout(activity).apply {
        id = R.id.root
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val title = Toolbar(activity, null).apply {
        id = R.id.configure_widget_title
        subtitle.isVisible = false
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val spinner = MaterialSpinnerView(activity, null).apply {
        id = R.id.theme
    }
    root.addView(
        spinner,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = Resources.dp(activity, 16f).toInt()
            rightMargin = Resources.dp(activity, 16f).toInt()
        }
    )

    val saveButton = Button(activity).apply {
        id = R.id.button_save
        text = activity.getString(R.string.save)
    }
    root.addView(
        saveButton,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START
            leftMargin = Resources.dp(activity, 16f).toInt()
            topMargin = Resources.dp(activity, 16f).toInt()
            rightMargin = Resources.dp(activity, 16f).toInt()
        }
    )

    return ToolWidgetConfigureViews(root, title, spinner, saveButton)
}
