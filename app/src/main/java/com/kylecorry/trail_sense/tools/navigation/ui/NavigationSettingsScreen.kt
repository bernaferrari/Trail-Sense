@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

/** The navigation settings with the app's quick actions and map layer sheet. */
@Composable
fun NavigationSettingsToolScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val quickActionOptions = remember(context) {
        Tools.getQuickActions(context).map { SettingsOption(it.id.toString(), it.name) }
    }
    val layerSheet = remember { MapLayersSheetState(NavigationToolRegistration.MAP_ID) }

    DisposableEffect(layerSheet) {
        onDispose {
            layerSheet.dismiss()
        }
    }

    NavigationSettingsScreen(
        quickActionOptions = quickActionOptions,
        onShowLayers = {
            layerSheet.dismiss()
            layerSheet.show()
        }
    )
    layerSheet.Content(
        context,
        { navigator.navigate(it) }
    )
}
