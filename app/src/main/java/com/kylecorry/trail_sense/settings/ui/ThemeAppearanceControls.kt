package com.kylecorry.trail_sense.settings.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.UserPreferences.Theme
import com.kylecorry.trail_sense.shared.views.TrailSenseColorStyle
import com.kylecorry.trail_sense.shared.views.TrailSensePalette
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAppearancePicker
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

/** When the app is light or dark. Night vision and the black background sit on top of it. */
enum class ThemeMode { System, Light, Dark, Sun }

/** The three choices behind the one stored theme. */
data class ThemeChoice(val mode: ThemeMode, val black: Boolean, val night: Boolean) {
    fun toTheme(): Theme = when {
        night -> Theme.Night
        mode == ThemeMode.Light -> Theme.Light
        mode == ThemeMode.Sun -> Theme.SunriseSunset
        mode == ThemeMode.Dark -> if (black) Theme.Black else Theme.Dark
        else -> if (black) Theme.SystemBlack else Theme.System
    }

    companion object {
        /** [lastTheme] is what night vision returns to when it's turned off. */
        fun from(theme: Theme, lastTheme: Theme): ThemeChoice {
            val base = if (theme == Theme.Night) lastTheme.takeIf { it != Theme.Night } ?: Theme.System else theme
            val mode = when (base) {
                Theme.Light -> ThemeMode.Light
                Theme.Dark, Theme.Black -> ThemeMode.Dark
                Theme.SunriseSunset -> ThemeMode.Sun
                else -> ThemeMode.System
            }
            return ThemeChoice(mode, base == Theme.Black || base == Theme.SystemBlack, theme == Theme.Night)
        }
    }
}

/**
 * Appearance, right on the Settings page (ChangeDetection's pattern): the theme as connected buttons, every palette
 * as a swatch (the wallpaper's colours first), and the four colour styles as one strip. Under them, the choices only
 * Trail Sense has, as switches: the black background (only in modes that can be dark), night vision (and its
 * fullscreen option while it's on) and the wallpaper compass colours (only with wallpaper colours). Each choice
 * applies at once; there is no separate Theme page.
 */
@Composable
fun ThemeAppearanceControls(
    prefs: UserPreferences,
    wallpaperAvailable: Boolean,
    nightFullscreen: Boolean,
    compassWallpaperColors: Boolean,
    onThemeChanged: (Theme) -> Unit,
    onPaletteChanged: () -> Unit,
    onNightFullscreenChanged: (Boolean) -> Unit,
    onCompassWallpaperColorsChanged: (Boolean) -> Unit
) {
    var choice by remember { mutableStateOf(ThemeChoice.from(prefs.theme, prefs.lastTheme)) }
    var palette by remember { mutableStateOf(prefs.palette) }
    var style by remember { mutableStateOf(prefs.colorStyle) }
    var fullscreen by remember { mutableStateOf(nightFullscreen) }
    var compassColors by remember { mutableStateOf(compassWallpaperColors) }
    fun update(next: ThemeChoice) {
        if (next == choice) return
        if (!choice.night) prefs.lastTheme = choice.toTheme()
        choice = next
        onThemeChanged(next.toTheme())
    }
    val modes = listOf(
        TrailSenseChoice(ThemeMode.System, stringResource(R.string.theme_mode_auto), Icons.Rounded.BrightnessAuto),
        TrailSenseChoice(ThemeMode.Light, stringResource(R.string.theme_light), Icons.Rounded.LightMode),
        TrailSenseChoice(ThemeMode.Dark, stringResource(R.string.theme_dark), Icons.Rounded.DarkMode),
        TrailSenseChoice(ThemeMode.Sun, stringResource(R.string.theme_mode_sun), Icons.Rounded.WbTwilight)
    )
    val palettes = TrailSensePalette.entries.filter { it != TrailSensePalette.Wallpaper || wallpaperAvailable }
    val paletteNames = mapOf(
        TrailSensePalette.Wallpaper to stringResource(R.string.theme_palette_wallpaper),
        TrailSensePalette.Orange to stringResource(R.string.theme_palette_orange),
        TrailSensePalette.Amber to stringResource(R.string.theme_palette_amber),
        TrailSensePalette.Lime to stringResource(R.string.theme_palette_lime),
        TrailSensePalette.Green to stringResource(R.string.theme_palette_green),
        TrailSensePalette.Teal to stringResource(R.string.theme_palette_teal),
        TrailSensePalette.Cyan to stringResource(R.string.theme_palette_cyan),
        TrailSensePalette.Blue to stringResource(R.string.theme_palette_blue),
        TrailSensePalette.Indigo to stringResource(R.string.theme_palette_indigo),
        TrailSensePalette.Purple to stringResource(R.string.theme_palette_purple),
        TrailSensePalette.Pink to stringResource(R.string.theme_palette_pink)
    )
    val styleNames = mapOf(
        TrailSenseColorStyle.Expressive to stringResource(R.string.color_style_expressive),
        TrailSenseColorStyle.Vibrant to stringResource(R.string.color_style_vibrant),
        TrailSenseColorStyle.Tonal to stringResource(R.string.color_style_tonal),
        TrailSenseColorStyle.Muted to stringResource(R.string.color_style_muted)
    )
    val sunSummary = stringResource(R.string.theme_mode_sun_summary)
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
    TrailSenseAppearancePicker(
        modes = modes,
        selectedMode = choice.mode,
        onModeSelected = { update(choice.copy(mode = it, night = false)) },
        palettes = palettes,
        selectedPalette = palette,
        onPaletteSelected = {
            palette = it
            prefs.palette = it
            onPaletteChanged()
        },
        paletteLabel = { paletteNames.getValue(it) },
        selectedStyle = style,
        onStyleSelected = {
            style = it
            prefs.colorStyle = it
            onPaletteChanged()
        },
        styleLabel = { styleNames.getValue(it) },
        styleTitle = stringResource(R.string.color_style)
    )
    TrailSenseRowGroup(footer = sunSummary.takeIf { choice.mode == ThemeMode.Sun && !choice.night }) {
        if (choice.mode != ThemeMode.Light && choice.mode != ThemeMode.Sun) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.theme_black_background),
                    summary = stringResource(R.string.theme_black_background_summary),
                    checked = choice.black && !choice.night,
                    enabled = !choice.night,
                    onCheckedChange = { update(choice.copy(black = it)) },
                    position = position
                )
            }
        }
        row { position ->
            SettingsSwitchRow(
                label = stringResource(R.string.theme_night_vision),
                summary = stringResource(R.string.theme_night_vision_summary),
                checked = choice.night,
                onCheckedChange = { update(choice.copy(night = it)) },
                position = position
            )
        }
        if (choice.night) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_night_mode_fullscreen_title),
                    summary = stringResource(R.string.pref_night_mode_fullscreen_summary),
                    checked = fullscreen,
                    onCheckedChange = {
                        fullscreen = it
                        onNightFullscreenChanged(it)
                    },
                    position = position
                )
            }
        }
        if (palette == TrailSensePalette.Wallpaper) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.compass_wallpaper_colors),
                    summary = stringResource(R.string.compass_wallpaper_colors_summary),
                    checked = compassColors,
                    onCheckedChange = {
                        compassColors = it
                        onCompassWallpaperColorsChanged(it)
                    },
                    position = position
                )
            }
        }
    }
    }
}
