package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage

/** A choice in a settings picker: the stored [value] and the [label] shown for it. */
internal data class SettingsOption(
    val value: String,
    val label: String
)

/** Every settings page: the family's page with its large title and a way back. */
@Composable
internal fun SettingsList(
    title: String,
    content: LazyListScope.() -> Unit
) {
    TrailSensePage(
        title = title,
        onBack = rememberGoBack(),
        backLabel = stringResource(R.string.back),
        content = content
    )
}

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

/** Opens the family's single-choice dialog over [options] and reports the stored value picked. */
internal fun Context.pickOption(
    title: String,
    options: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Pickers.item(
        this,
        title,
        options.map { it.label },
        options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    ) { index ->
        if (index != null) {
            onSelected(options[index].value)
        }
    }
}

internal fun optionLabel(options: List<SettingsOption>, selected: String): String {
    return options.firstOrNull { it.value == selected }?.label ?: selected
}

internal fun Context.getStringPreference(
    cache: IPreferences,
    @StringRes key: Int,
    default: String
): String {
    return cache.getString(getString(key)) ?: default
}

internal fun Context.getBoolean(
    cache: IPreferences,
    @StringRes key: Int,
    default: Boolean
): Boolean {
    return cache.getBoolean(getString(key)) ?: default
}

internal fun Context.putString(cache: IPreferences, @StringRes key: Int, value: String) {
    cache.putString(getString(key), value)
}

internal fun Context.putBoolean(cache: IPreferences, @StringRes key: Int, value: Boolean) {
    cache.putBoolean(getString(key), value)
}

/** A distance with its unit's usual decimals, or [none] when there is no distance. */
internal fun formatDistance(formatter: FormatService, distance: Distance?, none: String): String {
    return distance?.let {
        formatter.formatDistance(
            it,
            Units.getDecimalPlaces(it.units)
        )
    } ?: none
}

/** A distance with its unit's usual decimals, or a dash when there is none. */
internal fun Context.formatDistance(formatter: FormatService, distance: Distance?): String {
    return formatDistance(formatter, distance, getString(R.string.dash))
}

/** A choice with its current value on the right; tap to pick another from a dialog. */
@Composable
internal fun UnitPickerRow(
    label: String,
    options: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit,
    position: TrailSenseGroupPosition,
    icon: ImageVector? = null
) {
    val context = LocalContext.current
    SettingsPickerRow(
        label = label,
        selected = optionLabel(options, selected),
        onClick = {
            context.pickOption(label, options, selected, onSelected)
        },
        position = position,
        icon = icon
    )
}

/** A unit as one calm row: its name, and its current value on the right; tap to pick another. */
@Composable
internal fun UnitValueRow(
    label: String,
    options: List<SettingsOption>,
    selected: String,
    position: TrailSenseGroupPosition,
    onSelected: (String) -> Unit
) {
    val context = LocalContext.current
    TrailSenseListItem(
        title = label,
        trailingText = optionLabel(options, selected),
        onClick = { context.pickOption(label, options, selected, onSelected) },
        position = position
    )
}

/** A unit with two to four short options, all on view at once. */
@Composable
internal fun UnitChoiceRow(
    label: String,
    options: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit,
    position: TrailSenseGroupPosition,
    icon: ImageVector? = null
) {
    SettingsChoiceRow(
        label = label,
        options = remember(options) { options.map { TrailSenseChoice(it.value, it.label) } },
        selected = selected,
        onSelect = onSelected,
        position = position,
        icon = icon
    )
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
