package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences

/**
 * The app's colors: the wallpaper's (where the system has them; the default) or one of the family's seeds. Installs
 * that turned dynamic colors off before the palette existed keep Trail Sense's own orange.
 */
var UserPreferences.palette: TrailSensePalette
    get() {
        val stored = preferences.getString(PreferenceKeys.themePalette)
        val palette = when {
            stored != null -> TrailSensePalette.from(stored)
            preferences.getBoolean(PreferenceKeys.useDynamicColors) ?: true -> TrailSensePalette.Wallpaper
            else -> TrailSensePalette.Default
        }
        return if (palette == TrailSensePalette.Wallpaper && !device.hasDynamicColors) {
            TrailSensePalette.Default
        } else {
            palette
        }
    }
    set(value) {
        preferences.putString(PreferenceKeys.themePalette, value.id)
        preferences.putBoolean(PreferenceKeys.useDynamicColors, value == TrailSensePalette.Wallpaper)
    }

/** How the app's own colors become a palette; wallpaper colors are used as the system gives them. */
var UserPreferences.colorStyle: TrailSenseColorStyle
    get() = TrailSenseColorStyle.from(preferences.getString(PreferenceKeys.colorStyle))
    set(value) = preferences.putString(PreferenceKeys.colorStyle, value.id)

val UserPreferences.useDynamicColors: Boolean
    get() = palette == TrailSensePalette.Wallpaper
