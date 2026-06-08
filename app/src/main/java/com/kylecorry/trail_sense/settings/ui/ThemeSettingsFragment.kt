package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.android.material.color.DynamicColors
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme

class ThemeSettingsFragment : TrailSenseComposeFragment() {

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val prefs by lazy { UserPreferences(requireContext()) }

    @Composable
    override fun FragmentContent() {
                    val dynamicColorsAvailable = remember {
                        DynamicColors.isDynamicColorAvailable()
                    }
                    val themeOptions = remember {
                        options(R.array.theme_entries, R.array.theme_values)
                    }
                    val widgetThemeOptions = remember { widgetThemeOptions() }

                    var theme by remember {
                        mutableStateOf(getStringPreference(R.string.pref_theme, "system"))
                    }
                    var useDynamicColors by remember {
                        mutableStateOf(getBoolean(R.string.pref_use_dynamic_colors, true))
                    }
                    var useDynamicCompassColors by remember {
                        mutableStateOf(
                            getBoolean(
                                R.string.pref_use_dynamic_colors_on_compass,
                                false
                            )
                        )
                    }
                    var widgetTheme by remember {
                        mutableStateOf(getDefaultWidgetTheme())
                    }
                    var useCompactMode by remember {
                        mutableStateOf(prefs.useCompactMode)
                    }
                    var showAllBottomNavigationLabels by remember {
                        mutableStateOf(prefs.useShowAllBottomNavigationLabels)
                    }
                    var nightModeFullscreen by remember {
                        mutableStateOf(
                            getBoolean(
                                R.string.pref_night_mode_fullscreen,
                                false
                            )
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.pref_theme_title)
                                )
                            }
                            item("theme") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_theme_title),
                                    selected = optionLabel(themeOptions, theme),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_theme_title),
                                            themeOptions,
                                            theme
                                        ) {
                                            theme = it
                                            putString(R.string.pref_theme, it)
                                            requireMainActivity().reloadTheme()
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (dynamicColorsAvailable) {
                                item("dynamic_colors") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.dynamic_colors),
                                        checked = useDynamicColors,
                                        onCheckedChange = {
                                            useDynamicColors = it
                                            putBoolean(R.string.pref_use_dynamic_colors, it)
                                            requireMainActivity().reloadTheme()
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                                item("dynamic_compass_colors") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.compass_dynamic_colors),
                                        summary = stringResource(R.string.compass_dynamic_colors_summary),
                                        checked = useDynamicCompassColors,
                                        onCheckedChange = {
                                            useDynamicCompassColors = it
                                            putBoolean(
                                                R.string.pref_use_dynamic_colors_on_compass,
                                                it
                                            )
                                        },
                                        enabled = useDynamicColors,
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("widget_theme") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.default_widget_theme),
                                    selected = optionLabel(widgetThemeOptions, widgetTheme),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.default_widget_theme),
                                            widgetThemeOptions,
                                            widgetTheme
                                        ) {
                                            widgetTheme = it
                                            putString(R.string.pref_default_widget_theme, it)
                                            Tools.triggerWidgetUpdate(requireContext(), null)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("compact_mode") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.compact_mode),
                                    checked = useCompactMode,
                                    onCheckedChange = {
                                        useCompactMode = it
                                        requireMainActivity().changeBottomNavLabelsVisibility(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("nav_labels") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_show_all_bottom_navigation_labels),
                                    checked = showAllBottomNavigationLabels,
                                    onCheckedChange = {
                                        showAllBottomNavigationLabels = it
                                        requireMainActivity().showAllBottomNavLabels(it)
                                    },
                                    enabled = !useCompactMode,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("night_fullscreen") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_night_mode_fullscreen_title),
                                    summary = stringResource(R.string.pref_night_mode_fullscreen_summary),
                                    checked = nightModeFullscreen,
                                    onCheckedChange = {
                                        nightModeFullscreen = it
                                        putBoolean(R.string.pref_night_mode_fullscreen, it)
                                        requireMainActivity().reloadTheme()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
    }

    private fun pickOption(
        title: String,
        options: List<ThemeOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun options(entriesId: Int, valuesId: Int): List<ThemeOption> {
        val entries = resources.getStringArray(entriesId)
        val values = resources.getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            ThemeOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun widgetThemeOptions(): List<ThemeOption> {
        return listOf(
            ThemeOption(
                WidgetTheme.System.id.toString(),
                getString(R.string.theme_system)
            ),
            ThemeOption(
                WidgetTheme.TransparentBlack.id.toString(),
                getString(
                    R.string.theme_transparent_type,
                    getString(R.string.widget_theme_black_text)
                )
            ),
            ThemeOption(
                WidgetTheme.TransparentWhite.id.toString(),
                getString(
                    R.string.theme_transparent_type,
                    getString(R.string.widget_theme_white_text)
                )
            )
        )
    }

    private fun optionLabel(options: List<ThemeOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getDefaultWidgetTheme(): String {
        val key = getString(R.string.pref_default_widget_theme)
        val current = cache.getString(key)
        if (current != null) {
            return current
        }

        val default = WidgetTheme.System.id.toString()
        cache.putString(key, default)
        return default
    }

    private fun getStringPreference(key: Int, default: String): String {
        return cache.getString(getString(key)) ?: default
    }

    private fun getBoolean(key: Int, default: Boolean): Boolean {
        return cache.getBoolean(getString(key)) ?: default
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    private data class ThemeOption(
        val value: String,
        val label: String
    )
}
