package com.kylecorry.trail_sense.settings.ui

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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
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
    host: TrailSenseDestinationHost,
    title: String,
    content: LazyListScope.() -> Unit
) {
    TrailSensePage(
        title = title,
        onBack = host::goBack,
        backLabel = stringResource(R.string.back),
        content = content
    )
}

/** The options of a string-array preference: [valuesId] stored, [entriesId] shown. */
internal fun TrailSenseDestinationHost.settingsOptions(
    @ArrayRes entriesId: Int,
    @ArrayRes valuesId: Int
): List<SettingsOption> {
    val entries = getResources().getStringArray(entriesId)
    val values = getResources().getStringArray(valuesId)
    return values.mapIndexed { index, value ->
        SettingsOption(value, entries.getOrElse(index) { value })
    }
}

internal fun TrailSenseDestinationHost.pickOption(
    title: String,
    options: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Pickers.item(
        requireContext(),
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

internal fun TrailSenseDestinationHost.getStringPreference(
    cache: IPreferences,
    @StringRes key: Int,
    default: String
): String {
    return cache.getString(getString(key)) ?: default
}

internal fun TrailSenseDestinationHost.getBoolean(
    cache: IPreferences,
    @StringRes key: Int,
    default: Boolean
): Boolean {
    return cache.getBoolean(getString(key)) ?: default
}

internal fun TrailSenseDestinationHost.putString(cache: IPreferences, @StringRes key: Int, value: String) {
    cache.putString(getString(key), value)
}

internal fun TrailSenseDestinationHost.putBoolean(cache: IPreferences, @StringRes key: Int, value: Boolean) {
    cache.putBoolean(getString(key), value)
}

/** A distance with its unit's usual decimals, or a dash when there is none. */
internal fun TrailSenseDestinationHost.formatDistance(formatter: FormatService, distance: Distance?): String {
    return distance?.let {
        formatter.formatDistance(
            it,
            Units.getDecimalPlaces(it.units)
        )
    } ?: getString(R.string.dash)
}

/** A choice with its current value on the right; tap to pick another from a dialog. */
@Composable
internal fun UnitPickerRow(
    host: TrailSenseDestinationHost,
    label: String,
    options: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit,
    position: TrailSenseGroupPosition,
    icon: ImageVector? = null
) {
    SettingsPickerRow(
        label = label,
        selected = optionLabel(options, selected),
        onClick = {
            host.pickOption(label, options, selected, onSelected)
        },
        position = position,
        icon = icon
    )
}

/** A unit as one calm row: its name, and its current value on the right; tap to pick another. */
@Composable
internal fun UnitValueRow(
    host: TrailSenseDestinationHost,
    label: String,
    options: List<SettingsOption>,
    selected: String,
    position: TrailSenseGroupPosition,
    onSelected: (String) -> Unit
) {
    TrailSenseListItem(
        title = label,
        trailingText = optionLabel(options, selected),
        onClick = { host.pickOption(label, options, selected, onSelected) },
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
    private val host: TrailSenseDestinationHost,
    private val create: () -> MapLayersSheetState
) {
    private var sheet by mutableStateOf<MapLayersSheetState?>(null)

    fun show() {
        dismiss()
        sheet = create().also { it.show() }
    }

    fun dismiss() {
        sheet?.dismissAndUpdateWidgets(host.requireContext())
    }

    @Composable
    fun Content() {
        sheet?.Content(
            host.requireContext(),
            { host.findNavigator().navigate(it) },
            Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun rememberSettingsLayerSheet(
    host: TrailSenseDestinationHost,
    create: () -> MapLayersSheetState
): SettingsLayerSheet {
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheet = remember(host) { SettingsLayerSheet(host, create) }
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
