package com.kylecorry.trail_sense.settings.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape

private val SettingsInnerPosition = TrailSenseGroupPosition(isFirst = false, isLast = false)

@Composable
fun SingleSwitchSettingsScreen(
    title: String,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                )
            ) {
                item("header") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            top = TrailSenseSpacing.sm,
                            bottom = TrailSenseSpacing.md
                        )
                    )
                }
                item("switch") {
                    SettingsSwitchRow(
                        label = label,
                        summary = summary,
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        position = TrailSenseGroupPosition.Single
                    )
                }
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
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val containerColor by animateColorAsState(
        targetValue = if (checked && enabled) {
            lerp(baseColor, MaterialTheme.colorScheme.primaryContainer, 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "settingsSwitchContainer"
    )
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
                    style = MaterialTheme.typography.bodyLarge,
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = label,
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
