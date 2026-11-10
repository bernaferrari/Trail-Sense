package com.kylecorry.trail_sense.tools.tools.widgets

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.preferences.preferences
import com.kylecorry.luna.text.toLongCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolWidget

/** How a widget is drawn; the Glance content picks its colors from this (see ToolWidgetGlanceContent). */
enum class WidgetTheme(override val id: Long) : Identifiable {
    System(1),
    TransparentBlack(2),
    TransparentWhite(3),
}

class WidgetPreferences(
    private val context: Context,
    private val widget: ToolWidget,
    private val appWidgetId: Int
) {

    private val prefs = preferences(
        context,
        "${context.packageName}_widget_preferences",
        commitChanges = true
    )

    private val appPrefs = DependencyRegistry.get<PreferencesSubsystem>().preferences

    fun setTheme(theme: WidgetTheme) {
        prefs.putLong(getThemeKey(appWidgetId), theme.id)
        // Refresh the widget

    }

    fun getTheme(): WidgetTheme {
        val colorId =
            prefs.getLong(getThemeKey(appWidgetId))
                ?: appPrefs.getString(context.getString(R.string.pref_default_widget_theme))
                    ?.toLongCompat()
                ?: WidgetTheme.System.id
        return WidgetTheme.entries.withId(colorId) ?: WidgetTheme.System
    }

    fun clear() {
        prefs.remove(getThemeKey(appWidgetId))
    }

    private fun getThemeKey(appWidgetId: Int) = "pref_background_color_$appWidgetId"
}
