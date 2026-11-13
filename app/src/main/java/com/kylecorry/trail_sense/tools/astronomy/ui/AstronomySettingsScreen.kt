@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

/** The astronomy settings with the app's quick actions. */
@Composable
fun AstronomySettingsToolScreen() {
    val context = LocalContext.current
    val quickActionOptions = remember(context) {
        Tools.getQuickActions(context).map { SettingsOption(it.id.toString(), it.name) }
    }
    AstronomySettingsScreen(quickActionOptions)
}
