package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage

/** A choice in a settings picker: the stored [value] and the [label] shown for it. */
data class SettingsOption(
    val value: String,
    val label: String
)

/** Every settings page: the family's page with its large title and a way back. */
@Composable
fun SettingsList(
    title: String,
    content: LazyListScope.() -> Unit
) {
    TrailSensePage(
        title = title,
        onBack = rememberGoBack(),
        backLabel = appString(Res.string.back),
        content = content
    )
}

/** Opens the family's single-choice dialog over [options] and reports the stored value picked. */
fun pickOption(
    title: String,
    options: List<SettingsOption>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Pickers.item(
        AppHost.current(),
        title,
        options.map { it.label },
        options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    ) { index ->
        if (index != null) {
            onSelected(options[index].value)
        }
    }
}

fun optionLabel(options: List<SettingsOption>, selected: String): String {
    return options.firstOrNull { it.value == selected }?.label ?: selected
}

fun Preferences.stringOr(key: String, default: String): String {
    return getString(key) ?: default
}

fun Preferences.booleanOr(key: String, default: Boolean): Boolean {
    return getBoolean(key) ?: default
}

/** A distance with its unit's usual decimals, or [none] when there is no distance. */
fun formatDistance(formatter: FormatService, distance: Distance?, none: String): String {
    return distance?.let {
        formatter.formatDistance(
            it,
            Units.getDecimalPlaces(it.units)
        )
    } ?: none
}

/** A choice with its current value on the right; tap to pick another from a dialog. */
@Composable
fun UnitPickerRow(
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
            pickOption(label, options, selected, onSelected)
        },
        position = position,
        icon = icon
    )
}

/** A unit as one calm row: its name, and its current value on the right; tap to pick another. */
@Composable
fun UnitValueRow(
    label: String,
    options: List<SettingsOption>,
    selected: String,
    position: TrailSenseGroupPosition,
    onSelected: (String) -> Unit
) {
    TrailSenseListItem(
        title = label,
        trailingText = optionLabel(options, selected),
        onClick = { pickOption(label, options, selected, onSelected) },
        position = position
    )
}

/** A unit with two to four short options, all on view at once. */
@Composable
fun UnitChoiceRow(
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

/** A distance with its unit's usual decimals, or a dash when there is none. */
fun formatDistance(formatter: FormatService, distance: Distance?): String {
    return formatDistance(formatter, distance, getAppService<AppStrings>().get(Res.string.dash))
}
