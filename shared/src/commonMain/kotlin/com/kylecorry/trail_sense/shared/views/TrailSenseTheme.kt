package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

/**
 * The app's colors, as the rest of the family picks them: the wallpaper's colors first (where Android has them), then
 * ten seeds ordered warm to cool. [seed] is null for the wallpaper.
 */
enum class TrailSensePalette(val id: String, val seed: Color?) {
    Wallpaper("dynamic", null),
    Orange("orange", Color(0xFFFF5722)),
    Amber("amber", Color(0xFFFFC107)),
    Lime("lime", Color(0xFF8BC34A)),
    Green("green", Color(0xFF4CAF50)),
    Teal("teal", Color(0xFF009688)),
    Cyan("cyan", Color(0xFF00BCD4)),
    Blue("blue", Color(0xFF2196F3)),
    Indigo("indigo", Color(0xFF3F51B5)),
    Purple("purple", Color(0xFF9C27B0)),
    Pink("pink", Color(0xFFE91E63));

    companion object {
        /** Trail Sense's own color, used wherever the wallpaper's colors aren't available. */
        val Default = Orange

        fun from(id: String?): TrailSensePalette = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * How a chosen color becomes a whole palette: the same four styles as every app of the family. Wallpaper colors are
 * used exactly as Android gives them, so the style only shapes the app's own colors.
 */
enum class TrailSenseColorStyle(val id: String, val palette: PaletteStyle) {
    Expressive("expressive", PaletteStyle.Expressive),
    Vibrant("vibrant", PaletteStyle.Vibrant),
    Tonal("tonal", PaletteStyle.TonalSpot),
    Muted("muted", PaletteStyle.Cmf());

    companion object {
        fun from(id: String?): TrailSenseColorStyle = entries.firstOrNull { it.id == id } ?: Tonal
    }
}

/** Material 3 Expressive's own shape scheme. */
val TrailSenseExpressiveShapes = Shapes()

/**
 * Material's sizes with a heavier hierarchy, like the rest of the family: semibold display, headline and title styles
 * (large titles a touch tighter), medium labels. Material's tracking elsewhere keeps long translations readable.
 */
val TrailSenseExpressiveTypography: Typography = with(Typography()) {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(letterSpacing = 0.2.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Medium)
    )
}

/**
 * The scheme for a palette and style. [wallpaperScheme] (Android's own, when the wallpaper palette is chosen) wins
 * whatever the style. [black] turns dark surfaces true black for OLED screens and night use, keeping the containers a
 * step apart so cards still read.
 */
fun trailSenseColorScheme(
    palette: TrailSensePalette,
    style: TrailSenseColorStyle,
    darkTheme: Boolean,
    black: Boolean = false,
    wallpaperScheme: ColorScheme? = null
): ColorScheme {
    val base = wallpaperScheme ?: dynamicColorScheme(
        seedColor = (palette.seed ?: TrailSensePalette.Default.seed)!!,
        isDark = darkTheme,
        style = style.palette,
        specVersion = ColorSpec.SpecVersion.SPEC_2025
    )
    return if (black && darkTheme) base.toBlack() else base
}

private fun ColorScheme.toBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0E0E0F),
    surfaceContainer = Color(0xFF151516),
    surfaceContainerHigh = Color(0xFF1C1C1E),
    surfaceContainerHighest = Color(0xFF242426)
)

/**
 * Multiplatform Trail Sense theme. Android resolves the wallpaper scheme and the user's choices, then delegates here,
 * so components look the same on every target.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrailSenseTheme(
    palette: TrailSensePalette = TrailSensePalette.Default,
    style: TrailSenseColorStyle = TrailSenseColorStyle.Tonal,
    darkTheme: Boolean = isSystemInDarkTheme(),
    black: Boolean = false,
    wallpaperScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val scheme = remember(palette, style, darkTheme, black, wallpaperScheme) {
        trailSenseColorScheme(palette, style, darkTheme, black, wallpaperScheme)
    }
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = TrailSenseExpressiveTypography,
        content = content
    )
}
