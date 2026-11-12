package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator

// Settings helpers that read Android resources (string arrays) or open the Android map layer sheet.

/** The options of a string-array preference: [valuesId] stored, [entriesId] shown. */
internal fun Context.settingsOptions(
    @ArrayRes entriesId: Int,
    @ArrayRes valuesId: Int
): List<SettingsOption> {
    val entries = resources.getStringArray(entriesId)
    val values = resources.getStringArray(valuesId)
    return values.mapIndexed { index, value ->
        SettingsOption(value, entries.getOrElse(index) { value })
    }
}

/** The options of a string-array preference, read once for this screen. */
@Composable
internal fun rememberSettingsOptions(
    @ArrayRes entriesId: Int,
    @ArrayRes valuesId: Int
): List<SettingsOption> {
    val context = LocalContext.current
    return remember(context, entriesId, valuesId) { context.settingsOptions(entriesId, valuesId) }
}

/**
 * A map's layer sheet opened from a settings page. Opening it again starts afresh; it closes, updating the widgets,
 * when the page pauses.
 */
internal class SettingsLayerSheet(
    private val context: Context,
    private val navigator: TrailSenseNavigator,
    private val create: () -> MapLayersSheetState
) {
    private var sheet by mutableStateOf<MapLayersSheetState?>(null)

    fun show() {
        dismiss()
        sheet = create().also { it.show() }
    }

    fun dismiss() {
        sheet?.dismissAndUpdateWidgets(context)
    }

    @Composable
    fun Content() {
        sheet?.Content(
            context,
            { navigator.navigate(it) },
            Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun rememberSettingsLayerSheet(
    create: () -> MapLayersSheetState
): SettingsLayerSheet {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheet = remember(context, navigator) { SettingsLayerSheet(context, navigator, create) }
    DisposableEffect(lifecycleOwner, sheet) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                sheet.dismiss()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return sheet
}
