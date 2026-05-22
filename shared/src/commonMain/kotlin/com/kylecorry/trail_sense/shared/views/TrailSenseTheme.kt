package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Shared (KMP-ready) version of the Trail Sense design system.
 * Dynamic colors are Android-only for now (expect/actual will be added when CMP is enabled).
 */
private val TrailSenseLightColors = lightColorScheme(
    primary = Color(0xFFFF6D00),
    onPrimary = Color(0xFF351000),
    primaryContainer = Color(0x80FF6D00),
    onPrimaryContainer = Color(0xFF351000),
    secondaryContainer = Color(0x80FF6D00),
    tertiaryContainer = Color(0x80FF6D00),
    onSecondaryContainer = Color(0xFF351000),
    onTertiaryContainer = Color(0xFF351000)
)

private val TrailSenseDarkColors = darkColorScheme(
    primary = Color(0xFFFF6D00),
    onPrimary = Color(0xFF351000),
    primaryContainer = Color(0x80FF6D00),
    onPrimaryContainer = Color(0xFF351000),
    secondaryContainer = Color(0x80FF6D00),
    tertiaryContainer = Color(0x80FF6D00),
    onSecondaryContainer = Color(0xFF351000),
    onTertiaryContainer = Color(0xFF351000)
)

@Composable
fun TrailSenseTheme(
    content: @Composable () -> Unit
) {
    // For JVM / non-Android targets we default to system dark/light.
    // On Android the app module can still provide dynamic color overrides later.
    val useDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (useDarkTheme) TrailSenseDarkColors else TrailSenseLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
