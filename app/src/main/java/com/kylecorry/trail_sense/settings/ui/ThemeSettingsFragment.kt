package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.theme.ThemeRepository
import com.kylecorry.trail_sense.shared.views.TrailSenseColorScheme
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThemeSettingsFragment : AndromedaPreferenceFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.theme_preferences, rootKey)

        reloadThemeOnChange(list(R.string.pref_theme))

        val dynamicColorsSwitch = switch(R.string.pref_use_dynamic_colors)
        val dynamicCompassColorsSwitch = switch(R.string.pref_use_dynamic_colors_on_compass)
        dynamicColorsSwitch?.isVisible = DynamicColors.isDynamicColorAvailable()
        dynamicCompassColorsSwitch?.isVisible = DynamicColors.isDynamicColorAvailable()
        dynamicCompassColorsSwitch?.isEnabled = prefs.useDynamicColors
        dynamicColorsSwitch?.setOnPreferenceChangeListener { _, newValue ->
            val useDynamic = newValue as Boolean
            val scheme = if (useDynamic) TrailSenseColorScheme.Dynamic else TrailSenseColorScheme.Orange
            lifecycleScope.launch {
                ThemeRepository.getInstance(requireContext()).setColorScheme(scheme)
            }
            requireMainActivity().reloadTheme()
            dynamicCompassColorsSwitch?.isEnabled = prefs.useDynamicColors
            true
        }

        val accentColorPref = findPreference<androidx.preference.ListPreference>("pref_accent_color")
        if (accentColorPref != null) {
            val colorOptions = listOf(
                TrailSenseColorScheme.Orange to "Orange",
                TrailSenseColorScheme.Blue to "Blue",
                TrailSenseColorScheme.Green to "Green",
                TrailSenseColorScheme.Red to "Red",
                TrailSenseColorScheme.Purple to "Purple",
                TrailSenseColorScheme.Teal to "Teal",
                TrailSenseColorScheme.Dynamic to "Dynamic (wallpaper)"
            )
            accentColorPref.entries = colorOptions.map { it.second }.toTypedArray()
            accentColorPref.entryValues = colorOptions.map { TrailSenseColorScheme.toId(it.first).toString() }.toTypedArray()

            lifecycleScope.launch {
                val current = ThemeRepository.getInstance(requireContext()).colorScheme.first()
                accentColorPref.value = TrailSenseColorScheme.toId(current).toString()
            }

            accentColorPref.setOnPreferenceChangeListener { _, newValue ->
                val id = (newValue as String).toIntOrNull() ?: 0
                val scheme = TrailSenseColorScheme.fromId(id)
                lifecycleScope.launch {
                    ThemeRepository.getInstance(requireContext()).setColorScheme(scheme)
                }
                requireMainActivity().reloadTheme()
                true
            }
        }

        // Set Compact Mode
        val compactMode = switch(R.string.pref_use_compact_mode)
        val navLabels = switch(R.string.pref_use_bottom_navigation_labels)
        navLabels?.isEnabled = !prefs.useCompactMode
        compactMode?.setOnPreferenceChangeListener { _, checked ->
            requireMainActivity().changeBottomNavLabelsVisibility(checked as Boolean)
            navLabels?.isEnabled = !(checked as Boolean)
            true
        }
        navLabels?.setOnPreferenceChangeListener{_,checked->
            requireMainActivity().showAllBottomNavLabels(checked as Boolean)
            true
        }

        // Set Night Mode Fullscreen
        val nightModeFullscreen = switch(R.string.pref_night_mode_fullscreen)
        nightModeFullscreen?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivity().reloadTheme()
            true
        }

        // Widget default theme
        val widgetTheme = list(R.string.pref_default_widget_theme)!!
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
        widgetTheme.entries = items.map { it.second }.toTypedArray()
        widgetTheme.entryValues = items.map { it.first.id.toString() }.toTypedArray()
        // Save the default value
        if (DependencyRegistry.get<PreferencesSubsystem>().preferences.getString(widgetTheme.key) == null) {
            widgetTheme.value = WidgetTheme.System.id.toString()
        }

        onChange(widgetTheme) {
            Tools.triggerWidgetUpdate(requireContext(), null)
        }
    }

    private fun reloadThemeOnChange(pref: androidx.preference.Preference?) {
        pref?.setOnPreferenceChangeListener { _, _ ->
            requireMainActivity().reloadTheme()
            true
        }
    }

}
