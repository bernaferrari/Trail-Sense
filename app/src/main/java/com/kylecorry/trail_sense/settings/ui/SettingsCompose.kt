package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

private val SettingsInnerPosition = TrailSenseGroupPosition(isFirst = false, isLast = false)

/** A settings page holding one switch: the page's title, the switch as a single group, an optional footer. */
@Composable
fun SingleSwitchSettingsScreen(
    title: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    footer: String? = null,
    onBack: (() -> Unit)? = null
) {
    TrailSensePage(
        title = title,
        modifier = modifier,
        onBack = onBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("switch", footer = footer) {
            row { position ->
                SettingsSwitchRow(
                    label = label,
                    summary = summary,
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    position = position
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    position: TrailSenseGroupPosition = SettingsInnerPosition
) {
    // A checked switch says so itself: the row keeps the group's tone so a group reads as one object.
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f),
        shape = trailSenseGroupShape(position),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        }
    }
}

@Composable
fun SettingsActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    position: TrailSenseGroupPosition = SettingsInnerPosition
) {
    TrailSenseListItem(
        title = label,
        subtitle = summary,
        trailing = trailing ?: {
            Box(
                modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                contentAlignment = Alignment.Center
            ) {
                // The row is the button and already reads its label; the chevron is decoration.
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        onClick = if (enabled) onClick else null,
        animateEntrance = false,
        position = position,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
    )
}

@Composable
fun SettingsInfoRow(
    label: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    position: TrailSenseGroupPosition = SettingsInnerPosition
) {
    TrailSenseListItem(
        title = label,
        subtitle = summary,
        onClick = null,
        animateEntrance = false,
        position = position,
        modifier = modifier
    )
}

@Composable
fun SettingsPickerRow(
    label: String,
    selected: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    position: TrailSenseGroupPosition = SettingsInnerPosition
) {
    SettingsActionRow(
        label = label,
        summary = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        position = position
    )
}

/**
 * A setting with two to four short answers, all visible at once: the label over the family's connected choice.
 * Use it instead of a picker row when every option fits on one line.
 */
@Composable
fun <T> SettingsChoiceRow(
    label: String,
    options: List<TrailSenseChoice<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = SettingsInnerPosition
) {
    TrailSenseGroupCard(
        position = position,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.md,
            bottom = TrailSenseSpacing.lg
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = TrailSenseSpacing.md)
        )
        TrailSenseConnectedChoice(
            options = options,
            selected = selected,
            onSelect = onSelect,
            label = label
        )
    }
}

/**
 * A number chosen on a slider in place, its current value beside the label in tabular figures. [onValueChange]
 * fires once the finger lifts, so a drag stores one value rather than every step on the way.
 */
@Composable
fun SettingsSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    formatValue: (Float) -> String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    position: TrailSenseGroupPosition = SettingsInnerPosition
) {
    var current by remember(value) { mutableFloatStateOf(value) }
    TrailSenseGroupCard(
        position = position,
        modifier = modifier,
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            top = TrailSenseSpacing.md,
            bottom = TrailSenseSpacing.sm
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Text(
                text = formatValue(current),
                style = MaterialTheme.typography.titleMedium.merge(TextStyle(fontFeatureSettings = "tnum")),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onValueChange(current) },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
