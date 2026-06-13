package com.kylecorry.trail_sense.tools.tools.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch

class ToolWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

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
        val themeItems = getThemeItems()

        setContent {
            TrailSenseTheme {
                var selectedTheme by remember { mutableStateOf(prefs.getTheme()) }
                ToolWidgetConfigureScreen(
                    title = widget.name,
                    themeItems = themeItems,
                    selectedTheme = selectedTheme,
                    onThemeSelected = { selectedTheme = it },
                    onSave = {
                        saveWidgetTheme(widget, prefs, selectedTheme)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun getThemeItems(): List<WidgetThemeItem> {
        return listOf(
            WidgetThemeItem(WidgetTheme.System, getString(R.string.theme_system)),
            WidgetThemeItem(
                WidgetTheme.TransparentBlack,
                getString(
                    R.string.theme_transparent_type,
                    getString(R.string.widget_theme_black_text)
                )
            ),
            WidgetThemeItem(
                WidgetTheme.TransparentWhite,
                getString(
                    R.string.theme_transparent_type,
                    getString(R.string.widget_theme_white_text)
                )
            )
        )
    }

    private fun saveWidgetTheme(
        widget: ToolWidget,
        prefs: WidgetPreferences,
        theme: WidgetTheme
    ) {
        prefs.setTheme(theme)
        lifecycleScope.launch {
            val widgetProvider =
                widget.widgetClass.getConstructor().newInstance() as? AppWidgetBase
            widgetProvider?.let {
                AppWidgetBase.forceUpdate(
                    this@ToolWidgetConfigureActivity,
                    it
                )
            }

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}

private data class WidgetThemeItem(
    val theme: WidgetTheme,
    val label: String
)

@Composable
private fun ToolWidgetConfigureScreen(
    title: String,
    themeItems: List<WidgetThemeItem>,
    selectedTheme: WidgetTheme,
    onThemeSelected: (WidgetTheme) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(TrailSenseSpacing.lg)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
                    .testTag("configure_widget_title")
            )

            ThemeDropdown(
                label = stringResource(R.string.pref_theme_title),
                items = themeItems,
                selectedTheme = selectedTheme,
                onThemeSelected = onThemeSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.lg)
                    .testTag("theme")
            )

            TrailSenseButton(
                text = stringResource(R.string.save),
                onClick = onSave,
                modifier = Modifier
                    .padding(
                        start = TrailSenseSpacing.lg,
                        top = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg
                    )
                    .widthIn(min = 96.dp)
                    .testTag("button_save")
            )
        }
    }
}

@Composable
private fun ThemeDropdown(
    label: String,
    items: List<WidgetThemeItem>,
    selectedTheme: WidgetTheme,
    onThemeSelected: (WidgetTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val selectedLabel = items.firstOrNull { it.theme == selectedTheme }?.label
        ?: items.firstOrNull()?.label
        ?: ""

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = TrailSenseSpacing.xs)
        )
        OutlinedButton(
            onClick = { isExpanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedLabel,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onThemeSelected(item.theme)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToolWidgetConfigureScreenPreview() {
    TrailSenseTheme {
        ToolWidgetConfigureScreen(
            title = "Weather",
            themeItems = listOf(
                WidgetThemeItem(WidgetTheme.System, "System"),
                WidgetThemeItem(WidgetTheme.TransparentBlack, "Transparent black"),
                WidgetThemeItem(WidgetTheme.TransparentWhite, "Transparent white")
            ),
            selectedTheme = WidgetTheme.System,
            onThemeSelected = {},
            onSave = {}
        )
    }
}
